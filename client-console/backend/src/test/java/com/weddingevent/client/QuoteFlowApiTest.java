package com.weddingevent.client;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingevent.common.domain.ServicePackage;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.VendorProfile;
import com.weddingevent.common.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** FN-VND-03: the vendor quotes or declines, the couple accepts (→ Vendor Project) or cancels */
class QuoteFlowApiTest extends ClientIntegrationTest {

    private User couple;
    private String coupleToken;
    private VendorProfile vendor;
    private String vendorToken;
    private ServicePackage pkg;

    @BeforeEach
    void setUp() throws Exception {
        couple = createUser(Role.ROLE_COUPLE, true);
        coupleToken = token(couple);
        vendor = createVendor();
        vendorToken = token(users.findById(vendor.getUser().getId()).orElseThrow());
        pkg = createPackage(vendor, 20_000_000);
    }

    private String request(String extra) throws Exception {
        return read(body(mvc.perform(authed(post("/api/v1/client/quote-requests"), coupleToken)
                        .content(json("{'servicePackageId':'" + pkg.getId() + "'" + extra + "}")))
                .andExpect(status().isCreated())), "$.data.id");
    }

    private void quote(String id, long price) throws Exception {
        mvc.perform(authed(post("/api/v1/client/vendor/quote-requests/" + id + "/quote"), vendorToken)
                        .content(json("{'quotedPrice':" + price + "}")))
                .andExpect(status().isOk());
    }

    @Test
    void theVendorQuotesThenTheCoupleAcceptsAndAProjectIsCreated() throws Exception {
        String id = request(",'eventDate':'" + today().plusMonths(2) + "','venue':'Gem Center'");

        mvc.perform(authed(post("/api/v1/client/vendor/quote-requests/" + id + "/quote"), vendorToken)
                        .content(json("{'quotedPrice':18000000,'vendorNote':'Đã gồm vận chuyển nội thành'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("QUOTED"))
                .andExpect(jsonPath("$.data.quotedPrice").value(18_000_000))
                .andExpect(jsonPath("$.data.vendorNote").value("Đã gồm vận chuyển nội thành"))
                .andExpect(jsonPath("$.data.respondedAt").isString());
        mvc.perform(authed(get("/api/v1/client/notifications"), coupleToken))
                .andExpect(jsonPath("$.data.items[0].title").value("Đã có báo giá"));

        // A new quote replaces the old one
        quote(id, 17_000_000);

        mvc.perform(authed(post("/api/v1/client/quote-requests/" + id + "/accept"), coupleToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isString())
                .andExpect(jsonPath("$.data.quoteRequestId").value(id))
                .andExpect(jsonPath("$.data.coupleName").value(couple.getFullName()))
                .andExpect(jsonPath("$.data.packageName").value(pkg.getName()))
                .andExpect(jsonPath("$.data.eventDate").value(today().plusMonths(2).toString()))
                .andExpect(jsonPath("$.data.venue").value("Gem Center"))
                .andExpect(jsonPath("$.data.agreedPrice").value(17_000_000))
                .andExpect(jsonPath("$.data.status").value("UPCOMING"))
                .andExpect(jsonPath("$.data.cancelRequestedAt").doesNotExist());
        mvc.perform(authed(get("/api/v1/client/quote-requests/" + id), coupleToken))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
        mvc.perform(authed(get("/api/v1/client/notifications"), vendorToken))
                .andExpect(jsonPath("$.data.items[0].title").value("Báo giá đã được chấp nhận"));

        // Only one project per request, and an accepted request is final
        mvc.perform(authed(post("/api/v1/client/quote-requests/" + id + "/accept"), coupleToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
        mvc.perform(authed(post("/api/v1/client/vendor/quote-requests/" + id + "/quote"), vendorToken)
                        .content(json("{'quotedPrice':1}")))
                .andExpect(status().isConflict());
    }

    @Test
    void acceptingNeedsAQuoteAndAnEventDate() throws Exception {
        String id = request("");
        mvc.perform(authed(post("/api/v1/client/quote-requests/" + id + "/accept"), coupleToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        quote(id, 9_000_000);
        // The request had no date: the couple must give one now, not in the past
        mvc.perform(authed(post("/api/v1/client/quote-requests/" + id + "/accept"), coupleToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("eventDate"));
        mvc.perform(authed(post("/api/v1/client/quote-requests/" + id + "/accept"), coupleToken)
                        .content(json("{'eventDate':'" + today().minusDays(1) + "'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("eventDate"));
        mvc.perform(authed(post("/api/v1/client/quote-requests/" + id + "/accept"), coupleToken)
                        .content(json("{'eventDate':'" + today().plusDays(30) + "','venue':'White Palace'}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.eventDate").value(today().plusDays(30).toString()))
                .andExpect(jsonPath("$.data.venue").value("White Palace"));
    }

    @Test
    void theVendorDeclinesAndTheRequestIsClosed() throws Exception {
        String id = request("");
        mvc.perform(authed(post("/api/v1/client/vendor/quote-requests/" + id + "/decline"), vendorToken)
                        .content(json("{'vendorNote':'Ngày này bên mình đã kín lịch'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DECLINED"))
                .andExpect(jsonPath("$.data.vendorNote").value("Ngày này bên mình đã kín lịch"));
        mvc.perform(authed(get("/api/v1/client/notifications"), coupleToken))
                .andExpect(jsonPath("$.data.items[0].title").value("Yêu cầu báo giá bị từ chối"));

        mvc.perform(authed(post("/api/v1/client/vendor/quote-requests/" + id + "/decline"), vendorToken))
                .andExpect(status().isConflict());
        // A closed request no longer blocks a new one for the same package
        request("");
    }

    @Test
    void theCoupleCancelsAnOpenRequest() throws Exception {
        String id = request("");
        quote(id, 5_000_000);
        mvc.perform(authed(post("/api/v1/client/quote-requests/" + id + "/cancel"), coupleToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        mvc.perform(authed(post("/api/v1/client/quote-requests/" + id + "/cancel"), coupleToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void quoteValidation() throws Exception {
        String id = request("");
        mvc.perform(authed(post("/api/v1/client/vendor/quote-requests/" + id + "/quote"), vendorToken)
                        .content(json("{'quotedPrice':0,'vendorNote':'" + "x".repeat(1001) + "'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.containsInAnyOrder("quotedPrice", "vendorNote")));
        mvc.perform(authed(post("/api/v1/client/vendor/quote-requests/" + id + "/quote"), vendorToken).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("quotedPrice"));
    }

    @Test
    void onlyTheRequestsOwnersCanActOnIt() throws Exception {
        String id = request("");
        VendorProfile otherVendor = createVendor();
        String otherVendorToken = token(users.findById(otherVendor.getUser().getId()).orElseThrow());
        mvc.perform(authed(post("/api/v1/client/vendor/quote-requests/" + id + "/quote"), otherVendorToken)
                        .content(json("{'quotedPrice':1000000}")))
                .andExpect(status().isNotFound());
        mvc.perform(authed(post("/api/v1/client/vendor/quote-requests/" + id + "/decline"), otherVendorToken))
                .andExpect(status().isNotFound());

        quote(id, 1_000_000);
        String otherCouple = coupleToken();
        mvc.perform(authed(post("/api/v1/client/quote-requests/" + id + "/accept"), otherCouple))
                .andExpect(status().isNotFound());
        mvc.perform(authed(post("/api/v1/client/quote-requests/" + id + "/cancel"), otherCouple))
                .andExpect(status().isNotFound());

        // Each side only uses its own endpoints
        mvc.perform(authed(post("/api/v1/client/vendor/quote-requests/" + id + "/quote"), coupleToken)
                        .content(json("{'quotedPrice':1000000}")))
                .andExpect(status().isForbidden());
        mvc.perform(authed(post("/api/v1/client/quote-requests/" + id + "/accept"), vendorToken))
                .andExpect(status().isForbidden());
    }
}
