package com.weddingevent.client.catalog;

import com.weddingevent.common.api.PageResponse;
import com.weddingevent.common.domain.ServicePackage;
import com.weddingevent.common.domain.StudioSettings;
import com.weddingevent.common.domain.VendorProfile;
import com.weddingevent.common.domain.enums.ServiceCategory;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.ServicePackageRepository;
import com.weddingevent.common.repository.StudioSettingsRepository;
import com.weddingevent.common.repository.VendorProfileRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FN-SVC-01..02: active service packages of vendors and of the studio. Studio packages (no vendor) show the studio's
 * name/address from the settings and have no {@code providerId}, because the studio has no mobile account.
 */
@Service
public class CatalogService {

    public record ServiceDto(
            String id,
            String name,
            String providerId,
            String providerName,
            long price,
            Long discountPrice,
            double rating,
            int reviewCount,
            String address,
            String category,
            String thumbnailUrl,
            String description,
            String zaloUrl) {}

    private final ServicePackageRepository packages;
    private final VendorProfileRepository vendors;
    private final StudioSettingsRepository settings;

    public CatalogService(ServicePackageRepository packages, VendorProfileRepository vendors, StudioSettingsRepository settings) {
        this.packages = packages;
        this.vendors = vendors;
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public PageResponse<ServiceDto> search(String category, String keyword, int page, int size) {
        Set<ServiceCategory> categories = parseCategory(category);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        StudioSettings studio = studio();
        boolean studioMatches = kw.isEmpty() || studio.getName().toLowerCase(Locale.ROOT).contains(kw);
        Page<ServicePackage> result = packages.searchCatalog(categories, "%" + escapeLike(kw) + "%", studioMatches, PageRequest.of(page, size));

        Map<UUID, VendorProfile> vendorById = vendors.findAllByIdIn(result.getContent().stream()
                        .map(ServicePackage::getVendorId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(VendorProfile::getId, Function.identity()));
        return PageResponse.of(result, p -> toDto(p, vendorById.get(p.getVendorId()), studio));
    }

    @Transactional(readOnly = true)
    public ServiceDto get(UUID id) {
        ServicePackage p = packages.findById(id).filter(ServicePackage::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("dịch vụ", id));
        VendorProfile vendor = p.getVendorId() == null ? null
                : vendors.findAllByIdIn(List.of(p.getVendorId())).stream().findFirst().orElse(null);
        return toDto(p, vendor, studio());
    }

    private static ServiceDto toDto(ServicePackage p, VendorProfile vendor, StudioSettings studio) {
        return new ServiceDto(
                p.getId().toString(),
                p.getName(),
                vendor == null ? null : vendor.getUser().getId().toString(),
                vendor == null ? studio.getName() : vendor.getBusinessName(),
                p.getPrice().longValueExact(),
                p.getDiscountPrice() == null ? null : p.getDiscountPrice().longValueExact(),
                vendor == null ? 0.0 : vendor.getRating().doubleValue(),
                vendor == null ? 0 : vendor.getReviewCount(),
                vendor == null ? studio.getAddress() : vendor.getAddress(),
                p.getCategory().name().toLowerCase(Locale.ROOT),
                p.getThumbnailUrl(),
                p.getDescription(),
                zaloUrl(vendor == null ? studio.getZaloPhone() : vendor.getUser().getPhone()));
    }

    /** "Liên hệ đặt lịch" (FN-SVC-02, v1.1.3): the provider's Zalo; studio from the settings, vendor from its account phone */
    private static String zaloUrl(String phone) {
        return phone == null || phone.isBlank() ? null : "https://zalo.me/" + phone;
    }

    private StudioSettings studio() {
        return settings.findById((short) 1).orElseThrow();
    }

    private static Set<ServiceCategory> parseCategory(String category) {
        if (category == null || category.isBlank()) {
            return EnumSet.allOf(ServiceCategory.class);
        }
        try {
            return EnumSet.of(ServiceCategory.valueOf(category.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            throw AppException.invalidField("category", "Danh mục chỉ có thể là photo, decor, restaurant hoặc dress");
        }
    }

    /** {@code %} and {@code _} typed by the user are literal (escape character {@code \}) */
    static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
