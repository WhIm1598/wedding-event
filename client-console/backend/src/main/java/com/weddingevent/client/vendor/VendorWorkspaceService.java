package com.weddingevent.client.vendor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.weddingevent.client.project.VendorProjectService.VendorProjectDto;
import com.weddingevent.client.quote.QuoteRequestService.QuoteRequestDto;
import com.weddingevent.common.domain.ServicePackage;
import com.weddingevent.common.domain.VendorProfile;
import com.weddingevent.common.domain.enums.QuoteRequestStatus;
import com.weddingevent.common.domain.enums.ServiceCategory;
import com.weddingevent.common.domain.enums.VendorProjectStatus;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.QuoteRequestRepository;
import com.weddingevent.common.repository.ServicePackageRepository;
import com.weddingevent.common.repository.VendorProfileRepository;
import com.weddingevent.common.repository.VendorProjectRepository;
import com.weddingevent.common.repository.VendorProjectRepository.PackageProjectCount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FN-VND-05: the vendor's dashboard and its own service packages. Another provider's package → 404. */
@Service
public class VendorWorkspaceService {

    public record DashboardDto(long monthlyRevenue, long newRequestCount, double rating, List<QuoteRequestDto> quoteRequests,
            List<VendorProjectDto> upcoming) {}

    public record VendorServiceRequest(
            @NotBlank(message = "Tên gói không được để trống") @Size(max = 100, message = "Tên gói tối đa 100 ký tự") String name,
            @NotNull(message = "Vui lòng chọn danh mục")
            @Pattern(regexp = "(?i)photo|decor|restaurant|dress", message = "Danh mục chỉ có thể là photo, decor, restaurant hoặc dress")
            String category,
            @Size(max = 50, message = "Loại gói tối đa 50 ký tự") String type,
            @NotNull(message = "Vui lòng nhập giá") @Positive(message = "Giá phải lớn hơn 0") Long price,
            @Positive(message = "Giá sau giảm phải lớn hơn 0") Long discountPrice,
            @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự") String description,
            @Size(max = 20, message = "Tối đa 20 quyền lợi") List<@NotBlank @Size(max = 200, message = "Mỗi quyền lợi tối đa 200 ký tự") String> features,
            @Size(max = 500, message = "Đường dẫn ảnh tối đa 500 ký tự") String thumbnailUrl) {}

    public record ActiveRequest(@NotNull(message = "Vui lòng chọn bật hoặc tắt") @JsonProperty("isActive") Boolean isActive) {}

    public record VendorServiceDto(
            String id, String name, String category, String type, long price, Long discountPrice, String description,
            List<String> features, String thumbnailUrl, @JsonProperty("isActive") boolean isActive, long projectCount) {

        static VendorServiceDto of(ServicePackage p, long projectCount) {
            return new VendorServiceDto(p.getId().toString(), p.getName(), p.getCategory().name().toLowerCase(Locale.ROOT), p.getType(),
                    p.getPrice().longValueExact(), p.getDiscountPrice() == null ? null : p.getDiscountPrice().longValueExact(),
                    p.getDescription(), List.copyOf(p.getFeatures()), p.getThumbnailUrl(), p.isActive(), projectCount);
        }
    }

    private final VendorProfileRepository vendors;
    private final ServicePackageRepository packages;
    private final QuoteRequestRepository requests;
    private final VendorProjectRepository projects;
    private final Clock clock;

    public VendorWorkspaceService(VendorProfileRepository vendors, ServicePackageRepository packages, QuoteRequestRepository requests,
            VendorProjectRepository projects, Clock clock) {
        this.vendors = vendors;
        this.packages = packages;
        this.requests = requests;
        this.projects = projects;
        this.clock = clock;
    }

    /** monthlyRevenue = agreed prices of COMPLETED projects with an event date in the current month (business time zone) */
    @Transactional(readOnly = true)
    public DashboardDto dashboard(UUID vendorUserId) {
        VendorProfile vendor = vendorOf(vendorUserId);
        YearMonth month = YearMonth.from(LocalDate.now(clock));
        BigDecimal revenue = projects.completedRevenue(vendor.getId(), month.atDay(1), month.atEndOfMonth());
        return new DashboardDto(
                revenue.longValueExact(),
                requests.countByVendorIdAndStatus(vendor.getId(), QuoteRequestStatus.PENDING),
                vendor.getRating().doubleValue(),
                requests.findTop5ByVendorIdAndStatusOrderByCreatedAtDesc(vendor.getId(), QuoteRequestStatus.PENDING)
                        .stream().map(QuoteRequestDto::of).toList(),
                projects.findTop5ByVendorIdAndStatusOrderByEventDateAsc(vendor.getId(), VendorProjectStatus.UPCOMING)
                        .stream().map(VendorProjectDto::of).toList());
    }

    /** All the vendor's packages, hidden ones included, with their projects not CANCELLED */
    @Transactional(readOnly = true)
    public List<VendorServiceDto> services(UUID vendorUserId) {
        UUID vendorId = vendorOf(vendorUserId).getId();
        Map<UUID, Long> counts = projects.countByPackage(vendorId).stream()
                .collect(Collectors.toMap(PackageProjectCount::getPackageId, PackageProjectCount::getProjects));
        return packages.findAllByVendorIdOrderByCreatedAtDesc(vendorId).stream()
                .map(p -> VendorServiceDto.of(p, counts.getOrDefault(p.getId(), 0L)))
                .toList();
    }

    @Transactional
    public VendorServiceDto create(UUID vendorUserId, VendorServiceRequest req) {
        ServicePackage p = new ServicePackage();
        p.setVendorId(vendorOf(vendorUserId).getId());
        apply(p, req);
        return VendorServiceDto.of(packages.save(p), 0);
    }

    /** Existing quote requests and projects keep their own package name and price */
    @Transactional
    public VendorServiceDto update(UUID vendorUserId, UUID id, VendorServiceRequest req) {
        ServicePackage p = own(vendorUserId, id);
        apply(p, req);
        return VendorServiceDto.of(p, projectCount(p));
    }

    /** A hidden package leaves the catalog and takes no new quote requests; open requests and projects carry on */
    @Transactional
    public VendorServiceDto setActive(UUID vendorUserId, UUID id, boolean active) {
        ServicePackage p = own(vendorUserId, id);
        p.setActive(active);
        return VendorServiceDto.of(p, projectCount(p));
    }

    private long projectCount(ServicePackage p) {
        return projects.countByPackage(p.getVendorId()).stream()
                .filter(c -> c.getPackageId().equals(p.getId())).mapToLong(PackageProjectCount::getProjects).sum();
    }

    private static void apply(ServicePackage p, VendorServiceRequest req) {
        if (req.discountPrice() != null && req.discountPrice() >= req.price()) {
            throw AppException.invalidField("discountPrice", "Giá sau giảm phải thấp hơn giá gốc");
        }
        ServiceCategory category = ServiceCategory.valueOf(req.category().trim().toUpperCase(Locale.ROOT));
        p.setName(req.name().trim());
        p.setCategory(category);
        // service_packages.type is required; the category label stands in when the vendor leaves it empty
        p.setType(req.type() == null || req.type().isBlank() ? category.name().toLowerCase(Locale.ROOT) : req.type().trim());
        p.setPrice(BigDecimal.valueOf(req.price()));
        p.setDiscountPrice(req.discountPrice() == null ? null : BigDecimal.valueOf(req.discountPrice()));
        p.setDescription(req.description() == null || req.description().isBlank() ? null : req.description().trim());
        p.getFeatures().clear();
        if (req.features() != null) {
            req.features().forEach(f -> p.getFeatures().add(f.trim()));
        }
        p.setThumbnailUrl(req.thumbnailUrl() == null || req.thumbnailUrl().isBlank() ? null : req.thumbnailUrl().trim());
    }

    private ServicePackage own(UUID vendorUserId, UUID id) {
        return packages.findByIdAndVendorId(id, vendorOf(vendorUserId).getId())
                .orElseThrow(() -> new ResourceNotFoundException("gói dịch vụ", id));
    }

    private VendorProfile vendorOf(UUID vendorUserId) {
        return vendors.findByUserId(vendorUserId).orElseThrow(() -> new ResourceNotFoundException("hồ sơ Vendor", vendorUserId));
    }
}
