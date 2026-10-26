package com.weddingevent.client;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingevent.common.domain.ServicePackage;
import com.weddingevent.common.domain.VendorProfile;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** FN-VND-05: the vendor's dashboard and its own service packages (create, edit, discount, show/hide) */
class VendorWorkspaceApiTest extends ClientIntegrationTest {

    private VendorProfile vendor;
    private String vendorToken;

    @BeforeEach
    void setUp() throws Exception {
        vendor = createVendor();
        vendor.setRating(new BigDecimal("4.7"));
        vendorProfiles.save(vendor);
        vendorToken = token(users.findById(vendor.getUser().getId()).orElseThrow());
    }

    private String request(ServicePackage pkg, String couple) throws Exception {
        return read(body(mvc.perform(authed(post("/api/v1/client/quote-requests"), couple)
                        .content(json("{'servicePackageId':'" + pkg.getId() + "'}")))
                .andExpect(status().isCreated())), "$.data.id");
    }

    /** A project of {@code pkg} at {@code price} on {@code date}, moved to {@code status} (null = stays UPCOMING) */
    private String project(ServicePackage pkg, long price, LocalDate date, String status) throws Exception {
        String couple = coupleToken();
        String request = request(pkg, couple);
        mvc.perform(authed(post("/api/v1/client/vendor/quote-requests/" + request + "/quote"), vendorToken)
                        .content(json("{'quotedPrice':" + price + "}")))
                .andExpect(status().isOk());
        String id = read(body(mvc.perform(authed(post("/api/v1/client/quote-requests/" + request + "/accept"), couple)
                        .content(json("{'eventDate':'" + date + "'}")))
                .andExpect(status().isCreated())), "$.data.id");
        if (status != null) {
            mvc.perform(authed(patch("/api/v1/client/vendor/projects/" + id + "/status"), vendorToken)
                            .content(json("{'status':'" + status + "'}")))
                    .andExpect(status().isOk());
        }
        return id;
    }

    @Test
    void dashboardCountsCompletedWorkOfThisMonthAndShowsWhatIsNext() throws Exception {
        ServicePackage pkg = createPackage(vendor, 20_000_000);
        project(pkg, 10_000_000, today(), "COMPLETED");               // counts
        project(pkg, 7_000_000, today(), "COMPLETED");                // counts
        project(pkg, 50_000_000, today(), null);                      // not done yet
        project(pkg, 9_000_000, today(), "CANCELLED");                // cancelled
        project(pkg, 30_000_000, today().plusMonths(2), "COMPLETED"); // another month
        String soonest = project(pkg, 1_000_000, today(), null);
        String newest = request(pkg, coupleToken());
        request(pkg, coupleToken());

        mvc.perform(authed(get("/api/v1/client/vendor/dashboard"), vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.monthlyRevenue").value(17_000_000))
                .andExpect(jsonPath("$.data.newRequestCount").value(2))
                .andExpect(jsonPath("$.data.rating").value(4.7))
                .andExpect(jsonPath("$.data.quoteRequests.length()").value(2))
                .andExpect(jsonPath("$.data.quoteRequests[1].id").value(newest))
                .andExpect(jsonPath("$.data.quoteRequests[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.upcoming.length()").value(2))
                .andExpect(jsonPath("$.data.upcoming[*].id").value(hasItem(soonest)))
                .andExpect(jsonPath("$.data.upcoming[*].status").value(containsInAnyOrder("UPCOMING", "UPCOMING")));
    }

    @Test
    void anEmptyDashboard() throws Exception {
        mvc.perform(authed(get("/api/v1/client/vendor/dashboard"), vendorToken))
                .andExpect(jsonPath("$.data.monthlyRevenue").value(0))
                .andExpect(jsonPath("$.data.newRequestCount").value(0))
                .andExpect(jsonPath("$.data.quoteRequests").isEmpty())
                .andExpect(jsonPath("$.data.upcoming").isEmpty());
        mvc.perform(authed(get("/api/v1/client/vendor/dashboard"), coupleToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void theVendorCreatesEditsAndDiscountsItsPackages() throws Exception {
        String created = body(mvc.perform(authed(post("/api/v1/client/vendor/services"), vendorToken).content(json(
                        "{'name':'Trang trí cổng hoa','category':'decor','type':'Trọn gói','price':12000000,'discountPrice':10000000,"
                                + "'description':'Cổng hoa tươi','features':['Hoa tươi','Lắp đặt tận nơi'],"
                                + "'thumbnailUrl':'https://example.com/gate.jpg'}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Trang trí cổng hoa"))
                .andExpect(jsonPath("$.data.category").value("decor"))
                .andExpect(jsonPath("$.data.price").value(12_000_000))
                .andExpect(jsonPath("$.data.discountPrice").value(10_000_000))
                .andExpect(jsonPath("$.data.features[1]").value("Lắp đặt tận nơi"))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.projectCount").value(0)));
        String id = read(created, "$.data.id");

        // Couples see the new package and its discount in the catalog
        String couple = coupleToken();
        mvc.perform(authed(get("/api/v1/client/services/" + id), couple))
                .andExpect(jsonPath("$.data.providerName").value(vendor.getBusinessName()))
                .andExpect(jsonPath("$.data.discountPrice").value(10_000_000));

        // Editing; null drops the discount
        mvc.perform(authed(put("/api/v1/client/vendor/services/" + id), vendorToken).content(json(
                        "{'name':'Cổng hoa cao cấp','category':'decor','price':15000000,'discountPrice':null,'features':[]}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Cổng hoa cao cấp"))
                .andExpect(jsonPath("$.data.discountPrice").doesNotExist())
                .andExpect(jsonPath("$.data.features").isEmpty());
        mvc.perform(authed(get("/api/v1/client/services/" + id), couple))
                .andExpect(jsonPath("$.data.price").value(15_000_000))
                .andExpect(jsonPath("$.data.discountPrice").doesNotExist());
    }

    @Test
    void packageValidation() throws Exception {
        mvc.perform(authed(post("/api/v1/client/vendor/services"), vendorToken).content(json(
                        "{'name':'','category':'flowers','price':0,'type':'" + "x".repeat(51) + "','description':'" + "x".repeat(2001)
                                + "','thumbnailUrl':'" + "x".repeat(501) + "'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder(
                        "name", "category", "price", "type", "description", "thumbnailUrl")));
        mvc.perform(authed(post("/api/v1/client/vendor/services"), vendorToken).content(json(
                        "{'name':'Gói','category':'photo','price':5000000,'discountPrice':5000000}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("discountPrice"));
        String tooMany = "'f',".repeat(21);
        mvc.perform(authed(post("/api/v1/client/vendor/services"), vendorToken).content(json(
                        "{'name':'Gói','category':'photo','price':5000000,'features':[" + tooMany.substring(0, tooMany.length() - 1) + "]}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("features"));
        mvc.perform(authed(post("/api/v1/client/vendor/services"), vendorToken).content(json(
                        "{'name':'Gói','category':'photo','price':5000000,'features':['" + "x".repeat(201) + "']}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theListShowsHiddenPackagesWithTheirProjectCount() throws Exception {
        ServicePackage busy = createPackage(vendor, 8_000_000);
        project(busy, 8_000_000, today().plusDays(20), null);
        project(busy, 8_000_000, today().plusDays(21), "COMPLETED");
        project(busy, 8_000_000, today().plusDays(22), "CANCELLED"); // doesn't count
        ServicePackage hidden = createPackage(vendor, 3_000_000);

        mvc.perform(authed(patch("/api/v1/client/vendor/services/" + hidden.getId() + "/active"), vendorToken)
                        .content(json("{'isActive':false}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));

        mvc.perform(authed(get("/api/v1/client/vendor/services"), vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.id == '" + busy.getId() + "')].projectCount").value(2))
                .andExpect(jsonPath("$.data[?(@.id == '" + hidden.getId() + "')].isActive").value(false));

        // A hidden package leaves the catalog and takes no new requests
        String couple = coupleToken();
        mvc.perform(authed(get("/api/v1/client/services/" + hidden.getId()), couple))
                .andExpect(status().isNotFound());
        mvc.perform(authed(post("/api/v1/client/quote-requests"), couple)
                        .content(json("{'servicePackageId':'" + hidden.getId() + "'}")))
                .andExpect(status().isNotFound());
        mvc.perform(authed(get("/api/v1/client/services?size=100"), couple))
                .andExpect(jsonPath("$.data.items[*].id").value(not(hasItem(hidden.getId().toString()))));
    }

    @Test
    void aVendorOnlyManagesItsOwnPackages() throws Exception {
        ServicePackage others = createPackage(createVendor(), 5_000_000);
        ServicePackage studios = createPackage(null, 5_000_000);
        for (ServicePackage p : new ServicePackage[] {others, studios}) {
            mvc.perform(authed(put("/api/v1/client/vendor/services/" + p.getId()), vendorToken)
                            .content(json("{'name':'Của tôi','category':'photo','price':1000000}")))
                    .andExpect(status().isNotFound());
            mvc.perform(authed(patch("/api/v1/client/vendor/services/" + p.getId() + "/active"), vendorToken)
                            .content(json("{'isActive':false}")))
                    .andExpect(status().isNotFound());
        }
        mvc.perform(authed(get("/api/v1/client/vendor/services"), vendorToken))
                .andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(authed(post("/api/v1/client/vendor/services"), coupleToken())
                        .content(json("{'name':'Gói','category':'photo','price':1000000}")))
                .andExpect(status().isForbidden());
    }
}
