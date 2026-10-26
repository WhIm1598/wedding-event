package com.weddingevent.client;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingevent.common.domain.ServicePackage;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.VendorProfile;
import com.weddingevent.common.domain.enums.Role;
import org.junit.jupiter.api.Test;

/** FN-VND-01 (send a quote request) and FN-VND-02 (lists and details for the couple and the vendor) */
class QuoteRequestApiTest extends ClientIntegrationTest {

    private String send(String token, ServicePackage pkg) throws Exception {
        return read(body(mvc.perform(authed(post("/api/v1/client/quote-requests"), token)
                        .content(json("{'servicePackageId':'" + pkg.getId() + "'}")))
                .andExpect(status().isCreated())), "$.data.id");
    }

    private String vendorToken(VendorProfile vendor) throws Exception {
        return token(users.findById(vendor.getUser().getId()).orElseThrow());
    }

    @Test
    void aCoupleAsksAVendorForAQuoteAndTheVendorIsNotified() throws Exception {
        User couple = createUser(Role.ROLE_COUPLE, true);
        VendorProfile vendor = createVendor();
        ServicePackage pkg = createPackage(vendor, 15_000_000);

        mvc.perform(authed(post("/api/v1/client/quote-requests"), token(couple)).content(json(
                        "{'servicePackageId':'" + pkg.getId() + "','eventDate':'" + today().plusMonths(3)
                                + "','venue':'White Palace','guestCount':300,'message':'Cần cổng hoa và sân khấu'}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.coupleName").value(couple.getFullName()))
                .andExpect(jsonPath("$.data.packageId").value(pkg.getId().toString()))
                .andExpect(jsonPath("$.data.packageName").value(pkg.getName()))
                .andExpect(jsonPath("$.data.vendorName").value(vendor.getBusinessName()))
                .andExpect(jsonPath("$.data.eventDate").value(today().plusMonths(3).toString()))
                .andExpect(jsonPath("$.data.venue").value("White Palace"))
                .andExpect(jsonPath("$.data.guestCount").value(300))
                .andExpect(jsonPath("$.data.message").value("Cần cổng hoa và sân khấu"))
                .andExpect(jsonPath("$.data.quotedPrice").doesNotExist())
                .andExpect(jsonPath("$.data.createdAt").isString());

        mvc.perform(authed(get("/api/v1/client/notifications"), vendorToken(vendor)))
                .andExpect(jsonPath("$.data.items[0].title").value("Yêu cầu báo giá mới"))
                .andExpect(jsonPath("$.data.items[0].kind").value("message"));
    }

    @Test
    void onlyActiveVendorPackagesTakeQuoteRequests() throws Exception {
        String couple = coupleToken();
        mvc.perform(authed(post("/api/v1/client/quote-requests"), couple)
                        .content(json("{'servicePackageId':'" + createPackage(null, 5_000_000).getId() + "'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PACKAGE_NOT_QUOTABLE"));

        ServicePackage hidden = createPackage(createVendor(), 5_000_000);
        hidden.setActive(false);
        servicePackages.save(hidden);
        mvc.perform(authed(post("/api/v1/client/quote-requests"), couple)
                        .content(json("{'servicePackageId':'" + hidden.getId() + "'}")))
                .andExpect(status().isNotFound());
        mvc.perform(authed(post("/api/v1/client/quote-requests"), couple)
                        .content(json("{'servicePackageId':'" + java.util.UUID.randomUUID() + "'}")))
                .andExpect(status().isNotFound());
    }

    @Test
    void oneOpenRequestPerPackageAndCouple() throws Exception {
        String couple = coupleToken();
        ServicePackage pkg = createPackage(createVendor(), 8_000_000);
        String first = send(couple, pkg);

        mvc.perform(authed(post("/api/v1/client/quote-requests"), couple).content(json("{'servicePackageId':'" + pkg.getId() + "'}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUOTE_ALREADY_OPEN"))
                .andExpect(jsonPath("$.data.existingId").value(first));

        // Another couple may ask for the same package
        send(coupleToken(), pkg);
    }

    /** Concurrent sends may all pass the check; the unique index then turns the losers into 409, never 500 */
    @Test
    void concurrentSendsOpenOneRequestAndRefuseTheRest() throws Exception {
        String couple = coupleToken();
        ServicePackage pkg = createPackage(createVendor(), 8_000_000);
        int threads = 8;
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        try {
            java.util.List<java.util.concurrent.Future<Integer>> results = new java.util.ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return mvc.perform(authed(post("/api/v1/client/quote-requests"), couple)
                                    .content(json("{'servicePackageId':'" + pkg.getId() + "'}")))
                            .andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            java.util.List<Integer> statuses = new java.util.ArrayList<>();
            for (java.util.concurrent.Future<Integer> r : results) {
                statuses.add(r.get());
            }
            org.assertj.core.api.Assertions.assertThat(statuses).containsOnly(201, 409);
            org.assertj.core.api.Assertions.assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void sendValidation() throws Exception {
        String couple = coupleToken();
        String pkg = createPackage(createVendor(), 8_000_000).getId().toString();
        mvc.perform(authed(post("/api/v1/client/quote-requests"), couple).content(json(
                        "{'guestCount':0,'venue':'" + "x".repeat(256) + "','message':'" + "x".repeat(1001) + "'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.containsInAnyOrder(
                        "servicePackageId", "guestCount", "venue", "message")));
        mvc.perform(authed(post("/api/v1/client/quote-requests"), couple)
                        .content(json("{'servicePackageId':'" + pkg + "','guestCount':5001}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("guestCount"));
        mvc.perform(authed(post("/api/v1/client/quote-requests"), couple)
                        .content(json("{'servicePackageId':'" + pkg + "','eventDate':'" + today().minusDays(1) + "'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("eventDate"));
        // Today is allowed
        mvc.perform(authed(post("/api/v1/client/quote-requests"), couple)
                        .content(json("{'servicePackageId':'" + pkg + "','eventDate':'" + today() + "'}")))
                .andExpect(status().isCreated());
    }

    @Test
    void onlyCouplesSendAndOnlyVendorsReadTheVendorList() throws Exception {
        VendorProfile vendor = createVendor();
        String vendorToken = vendorToken(vendor);
        mvc.perform(authed(post("/api/v1/client/quote-requests"), vendorToken)
                        .content(json("{'servicePackageId':'" + createPackage(createVendor(), 1_000_000).getId() + "'}")))
                .andExpect(status().isForbidden());
        mvc.perform(authed(get("/api/v1/client/vendor/quote-requests"), coupleToken()))
                .andExpect(status().isForbidden());
        mvc.perform(authed(get("/api/v1/client/quote-requests"), vendorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void eachSideSeesOnlyItsOwnRequests() throws Exception {
        VendorProfile vendor = createVendor();
        VendorProfile otherVendor = createVendor();
        String couple = coupleToken();
        String mine = send(couple, createPackage(vendor, 10_000_000));
        String otherCouples = send(coupleToken(), createPackage(vendor, 12_000_000));
        String toOtherVendor = send(couple, createPackage(otherVendor, 9_000_000));

        mvc.perform(authed(get("/api/v1/client/quote-requests?page=0&size=20"), couple))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(toOtherVendor)) // newest first
                .andExpect(jsonPath("$.data.items[1].id").value(mine))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));

        String vendorToken = vendorToken(vendor);
        mvc.perform(authed(get("/api/v1/client/vendor/quote-requests?status=PENDING"), vendorToken))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.items[*].id").value(not(hasItem(toOtherVendor))));
        mvc.perform(authed(get("/api/v1/client/vendor/quote-requests?status=QUOTED"), vendorToken))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mvc.perform(authed(get("/api/v1/client/vendor/quote-requests/" + otherCouples), vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(otherCouples));
        mvc.perform(authed(get("/api/v1/client/vendor/quote-requests/" + toOtherVendor), vendorToken))
                .andExpect(status().isNotFound());
        mvc.perform(authed(get("/api/v1/client/quote-requests/" + mine), couple))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
        mvc.perform(authed(get("/api/v1/client/quote-requests/" + otherCouples), couple))
                .andExpect(status().isNotFound());
    }

    @Test
    void listParameterValidation() throws Exception {
        String couple = coupleToken();
        mvc.perform(authed(get("/api/v1/client/quote-requests?status=OPEN"), couple))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
        mvc.perform(authed(get("/api/v1/client/quote-requests?size=101"), couple))
                .andExpect(status().isBadRequest());
    }
}
