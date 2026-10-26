package com.weddingevent.client;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingevent.common.domain.ServicePackage;
import com.weddingevent.common.domain.StudioSettings;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.VendorProfile;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.domain.enums.ServiceCategory;
import com.weddingevent.common.repository.ServicePackageRepository;
import com.weddingevent.common.repository.StudioSettingsRepository;
import com.weddingevent.common.repository.VendorProfileRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** FN-SVC-01..02. Each test uses a unique keyword, because the catalog is shared by all tests. */
class CatalogApiTest extends ClientIntegrationTest {

    @Autowired
    ServicePackageRepository packages;

    @Autowired
    VendorProfileRepository vendors;

    @Autowired
    StudioSettingsRepository studioSettings;

    private VendorProfile vendor(String businessName) {
        User u = createUser(Role.ROLE_VENDOR, true);
        VendorProfile v = new VendorProfile();
        v.setUser(u);
        v.setBusinessName(businessName);
        v.setAddress("Quận 1, TP.HCM");
        v.setRating(new BigDecimal("4.8"));
        v.setReviewCount(85);
        return vendors.save(v);
    }

    private ServicePackage pkg(VendorProfile vendor, String name, ServiceCategory category, long price, boolean active) {
        ServicePackage p = new ServicePackage();
        p.setVendorId(vendor == null ? null : vendor.getId());
        p.setName(name);
        p.setType("Trọn gói");
        p.setCategory(category);
        p.setPrice(BigDecimal.valueOf(price));
        p.setDescription("Mô tả " + name);
        p.setThumbnailUrl("https://example.com/" + UUID.randomUUID() + ".jpg");
        p.setActive(active);
        return packages.save(p);
    }

    @Test
    void searchMatchesPackageOrProviderNameAndReturnsTheSpecPageShape() throws Exception {
        String key = UUID.randomUUID().toString().substring(0, 8);
        VendorProfile decor = vendor("Dreamy Decor " + key);
        ServicePackage a = pkg(decor, "Trang trí hoa tươi", ServiceCategory.DECOR, 25_000_000, true);
        pkg(decor, "Cổng hoa", ServiceCategory.DECOR, 5_000_000, true);
        pkg(null, "Gói ảnh " + key, ServiceCategory.PHOTO, 12_000_000, true);
        pkg(decor, "Gói đã ẩn", ServiceCategory.DECOR, 1_000_000, false);
        String token = coupleToken();

        mvc.perform(authed(get("/api/v1/client/services?keyword=" + key.toUpperCase() + "&size=2"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2));

        mvc.perform(authed(get("/api/v1/client/services?keyword=" + key + "&category=decor"), token))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.items[?(@.id=='" + a.getId() + "')].providerName").value("Dreamy Decor " + key))
                .andExpect(jsonPath("$.data.items[?(@.id=='" + a.getId() + "')].providerId").value(decor.getUser().getId().toString()))
                .andExpect(jsonPath("$.data.items[?(@.id=='" + a.getId() + "')].price").value(25_000_000))
                .andExpect(jsonPath("$.data.items[?(@.id=='" + a.getId() + "')].rating").value(4.8))
                .andExpect(jsonPath("$.data.items[?(@.id=='" + a.getId() + "')].reviewCount").value(85))
                .andExpect(jsonPath("$.data.items[?(@.id=='" + a.getId() + "')].address").value("Quận 1, TP.HCM"))
                .andExpect(jsonPath("$.data.items[?(@.id=='" + a.getId() + "')].category").value("decor"))
                .andExpect(jsonPath("$.data.items[?(@.id=='" + a.getId() + "')].thumbnailUrl").value(a.getThumbnailUrl()))
                .andExpect(jsonPath("$.data.items[?(@.id=='" + a.getId() + "')].description").value("Mô tả Trang trí hoa tươi"));
    }

    @Test
    void studioPackagesShowTheStudioAsProvider() throws Exception {
        String key = UUID.randomUUID().toString().substring(0, 8);
        ServicePackage studio = pkg(null, "Pre-wedding " + key, ServiceCategory.PHOTO, 15_000_000, true);
        mvc.perform(authed(get("/api/v1/client/services/" + studio.getId()), coupleToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(studio.getId().toString()))
                .andExpect(jsonPath("$.data.providerName").value("LUMIÈRE STUDIOS"))
                .andExpect(jsonPath("$.data.providerId").doesNotExist())
                .andExpect(jsonPath("$.data.category").value("photo"))
                .andExpect(jsonPath("$.data.rating").value(0.0))
                .andExpect(jsonPath("$.data.reviewCount").value(0));
    }

    /** FN-SVC-01/02 (v1.1.3): "Liên hệ đặt lịch" opens the provider's Zalo; no number → no link */
    @Test
    void contactLinksOpenTheProvidersZalo() throws Exception {
        String couple = coupleToken();
        VendorProfile withPhone = vendor("Zalo " + UUID.randomUUID().toString().substring(0, 6));
        User vendorUser = users.findById(withPhone.getUser().getId()).orElseThrow();
        vendorUser.setPhone("0912345678");
        users.save(vendorUser);
        ServicePackage vendorPkg = pkg(withPhone, "Có Zalo", ServiceCategory.DECOR, 1_000_000, true);
        ServicePackage noPhonePkg = pkg(vendor("Không SĐT"), "Không Zalo", ServiceCategory.DECOR, 1_000_000, true);
        ServicePackage studioPkg = pkg(null, "Studio Zalo", ServiceCategory.PHOTO, 1_000_000, true);

        mvc.perform(authed(get("/api/v1/client/services/" + vendorPkg.getId()), couple))
                .andExpect(jsonPath("$.data.zaloUrl").value("https://zalo.me/0912345678"));
        mvc.perform(authed(get("/api/v1/client/services/" + noPhonePkg.getId()), couple))
                .andExpect(jsonPath("$.data.zaloUrl").doesNotExist());

        StudioSettings studio = studioSettings.findById(StudioSettings.SINGLETON_ID).orElseThrow();
        studio.setZaloPhone("0987654321");
        studioSettings.save(studio);
        try {
            mvc.perform(authed(get("/api/v1/client/services/" + studioPkg.getId()), couple))
                    .andExpect(jsonPath("$.data.zaloUrl").value("https://zalo.me/0987654321"));
        } finally {
            studio.setZaloPhone(null);
            studioSettings.save(studio);
        }
        mvc.perform(authed(get("/api/v1/client/services/" + studioPkg.getId()), couple))
                .andExpect(jsonPath("$.data.zaloUrl").doesNotExist());
    }

    @Test
    void keywordWildcardsAreLiteral() throws Exception {
        String key = UUID.randomUUID().toString().substring(0, 6);
        pkg(null, "Ưu đãi_" + key, ServiceCategory.DRESS, 8_000_000, true);
        String token = coupleToken();
        mvc.perform(authed(get("/api/v1/client/services?keyword=_" + key), token))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mvc.perform(authed(get("/api/v1/client/services?keyword=%" + key), token))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void inactiveOrUnknownPackagesAreNotFound() throws Exception {
        ServicePackage hidden = pkg(vendor(unique("Ẩn")), unique("Gói ẩn"), ServiceCategory.RESTAURANT, 1, false);
        String token = token(createUser(Role.ROLE_VENDOR, true)); // vendors can browse too
        mvc.perform(authed(get("/api/v1/client/services/" + hidden.getId()), token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(authed(get("/api/v1/client/services/" + UUID.randomUUID()), token)).andExpect(status().isNotFound());
    }

    @Test
    void queryParameterValidation() throws Exception {
        String token = coupleToken();
        mvc.perform(authed(get("/api/v1/client/services?category=cars"), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("category"));
        mvc.perform(authed(get("/api/v1/client/services?size=101"), token)).andExpect(status().isBadRequest());
        mvc.perform(authed(get("/api/v1/client/services?page=-1"), token)).andExpect(status().isBadRequest());
        mvc.perform(authed(get("/api/v1/client/services?category=&keyword="), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(10));
    }
}
