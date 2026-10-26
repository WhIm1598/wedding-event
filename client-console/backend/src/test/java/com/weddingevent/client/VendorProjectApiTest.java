package com.weddingevent.client;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingevent.common.domain.ServicePackage;
import com.weddingevent.common.domain.VendorProfile;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** FN-VND-04: the vendor's projects and their status; the couple's projects and cancellation requests */
class VendorProjectApiTest extends ClientIntegrationTest {

    private String coupleToken;
    private String vendorToken;
    private ServicePackage pkg;

    @BeforeEach
    void setUp() throws Exception {
        coupleToken = coupleToken();
        VendorProfile vendor = createVendor();
        vendorToken = token(users.findById(vendor.getUser().getId()).orElseThrow());
        pkg = createPackage(vendor, 20_000_000);
    }

    /** Request → quote → accept, as a couple of this test; returns the project id */
    private String project(String couple, LocalDate eventDate) throws Exception {
        String request = read(body(mvc.perform(authed(post("/api/v1/client/quote-requests"), couple)
                        .content(json("{'servicePackageId':'" + pkg.getId() + "','eventDate':'" + eventDate + "'}")))
                .andExpect(status().isCreated())), "$.data.id");
        mvc.perform(authed(post("/api/v1/client/vendor/quote-requests/" + request + "/quote"), vendorToken)
                        .content(json("{'quotedPrice':15000000}")))
                .andExpect(status().isOk());
        return read(body(mvc.perform(authed(post("/api/v1/client/quote-requests/" + request + "/accept"), couple))
                .andExpect(status().isCreated())), "$.data.id");
    }

    private void move(String id, String status) throws Exception {
        mvc.perform(authed(patch("/api/v1/client/vendor/projects/" + id + "/status"), vendorToken)
                        .content(json("{'status':'" + status + "'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(status));
    }

    @Test
    void theVendorSeesItsProjectsByStatusInDateOrder() throws Exception {
        String later = project(coupleToken, today().plusMonths(3));
        String sooner = project(coupleToken(), today().plusMonths(1));
        String done = project(coupleToken(), today().plusDays(10));
        String doneEarlier = project(coupleToken(), today().plusDays(5));
        move(done, "COMPLETED");
        move(doneEarlier, "COMPLETED");

        mvc.perform(authed(get("/api/v1/client/vendor/projects?status=UPCOMING"), vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(sooner))
                .andExpect(jsonPath("$.data.items[1].id").value(later))
                .andExpect(jsonPath("$.data.items[0].agreedPrice").value(15_000_000));
        // Finished projects: most recent first
        mvc.perform(authed(get("/api/v1/client/vendor/projects?status=COMPLETED"), vendorToken))
                .andExpect(jsonPath("$.data.items[0].id").value(done))
                .andExpect(jsonPath("$.data.items[1].id").value(doneEarlier));
        mvc.perform(authed(get("/api/v1/client/vendor/projects"), vendorToken))
                .andExpect(jsonPath("$.data.totalElements").value(4));
        mvc.perform(authed(get("/api/v1/client/vendor/projects?status=DONE"), vendorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));

        // Another vendor sees none of them
        String otherVendor = token(users.findById(createVendor().getUser().getId()).orElseThrow());
        mvc.perform(authed(get("/api/v1/client/vendor/projects"), otherVendor))
                .andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(authed(patch("/api/v1/client/vendor/projects/" + later + "/status"), otherVendor)
                        .content(json("{'status':'IN_PROGRESS'}")))
                .andExpect(status().isNotFound());
    }

    @Test
    void statusFollowsTheStateMachineAndTheCoupleHearsTheEnd() throws Exception {
        String id = project(coupleToken, today().plusMonths(2));
        move(id, "IN_PROGRESS");
        mvc.perform(authed(patch("/api/v1/client/vendor/projects/" + id + "/status"), vendorToken)
                        .content(json("{'status':'UPCOMING'}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
        move(id, "COMPLETED");
        mvc.perform(authed(get("/api/v1/client/notifications"), coupleToken))
                .andExpect(jsonPath("$.data.items[0].title").value("Dự án đã hoàn thành"));
        mvc.perform(authed(patch("/api/v1/client/vendor/projects/" + id + "/status"), vendorToken)
                        .content(json("{'status':'CANCELLED'}")))
                .andExpect(status().isConflict());
        mvc.perform(authed(patch("/api/v1/client/vendor/projects/" + id + "/status"), vendorToken).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theCoupleAsksToCancelAndTheVendorApproves() throws Exception {
        String id = project(coupleToken, today().plusMonths(2));
        mvc.perform(authed(get("/api/v1/client/vendor-projects"), coupleToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(id))
                .andExpect(jsonPath("$.data.items[0].vendorName").isString());

        mvc.perform(authed(post("/api/v1/client/vendor-projects/" + id + "/cancel-request"), coupleToken)
                        .content(json("{'reason':'Đổi địa điểm tổ chức'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UPCOMING"))
                .andExpect(jsonPath("$.data.cancelRequestedAt").isString())
                .andExpect(jsonPath("$.data.cancelReason").value("Đổi địa điểm tổ chức"));
        mvc.perform(authed(get("/api/v1/client/notifications"), vendorToken))
                .andExpect(jsonPath("$.data.items[0].title").value("Cặp đôi xin hủy dự án"));
        mvc.perform(authed(post("/api/v1/client/vendor-projects/" + id + "/cancel-request"), coupleToken)
                        .content(json("{'reason':'Lần nữa'}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        mvc.perform(authed(post("/api/v1/client/vendor/projects/" + id + "/cancel-request/approve"), vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        mvc.perform(authed(get("/api/v1/client/notifications"), coupleToken))
                .andExpect(jsonPath("$.data.items[0].title").value("Dự án đã được hủy"));
    }

    @Test
    void theVendorRejectsOrMovesOnAndTheProjectContinues() throws Exception {
        String id = project(coupleToken, today().plusMonths(2));
        mvc.perform(authed(post("/api/v1/client/vendor/projects/" + id + "/cancel-request/reject"), vendorToken))
                .andExpect(status().isConflict()); // nothing to reject

        mvc.perform(authed(post("/api/v1/client/vendor-projects/" + id + "/cancel-request"), coupleToken)
                        .content(json("{'reason':'Đổi ý'}")))
                .andExpect(status().isOk());
        mvc.perform(authed(post("/api/v1/client/vendor/projects/" + id + "/cancel-request/reject"), vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UPCOMING"))
                .andExpect(jsonPath("$.data.cancelRequestedAt").doesNotExist());
        mvc.perform(authed(get("/api/v1/client/notifications"), coupleToken))
                .andExpect(jsonPath("$.data.items[0].title").value("Yêu cầu hủy dự án bị từ chối"));

        // Asking again, then the vendor starts the work: that rejects the request too
        mvc.perform(authed(post("/api/v1/client/vendor-projects/" + id + "/cancel-request"), coupleToken)
                        .content(json("{'reason':'Đổi ý lần 2'}")))
                .andExpect(status().isOk());
        move(id, "IN_PROGRESS");
        mvc.perform(authed(get("/api/v1/client/vendor-projects"), coupleToken))
                .andExpect(jsonPath("$.data.items[0].cancelRequestedAt").doesNotExist());
        mvc.perform(authed(get("/api/v1/client/notifications"), coupleToken))
                .andExpect(jsonPath("$.data.items[0].title").value("Yêu cầu hủy dự án bị từ chối"));
    }

    @Test
    void cancelRequestRulesAndScoping() throws Exception {
        String id = project(coupleToken, today().plusMonths(2));
        mvc.perform(authed(post("/api/v1/client/vendor-projects/" + id + "/cancel-request"), coupleToken).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
        mvc.perform(authed(post("/api/v1/client/vendor-projects/" + id + "/cancel-request"), coupleToken)
                        .content(json("{'reason':'" + "x".repeat(501) + "'}")))
                .andExpect(status().isBadRequest());
        mvc.perform(authed(post("/api/v1/client/vendor-projects/" + id + "/cancel-request"), coupleToken())
                        .content(json("{'reason':'Không phải của tôi'}")))
                .andExpect(status().isNotFound());
        mvc.perform(authed(get("/api/v1/client/vendor-projects"), vendorToken))
                .andExpect(status().isForbidden());
        mvc.perform(authed(get("/api/v1/client/vendor/projects"), coupleToken))
                .andExpect(status().isForbidden());

        move(id, "COMPLETED");
        mvc.perform(authed(post("/api/v1/client/vendor-projects/" + id + "/cancel-request"), coupleToken)
                        .content(json("{'reason':'Muộn rồi'}")))
                .andExpect(status().isConflict());
    }
}
