package com.weddingevent.admin.packages;

import static com.weddingevent.admin.security.SecurityConfig.ADMIN;

import com.weddingevent.common.api.ApiResponse;
import com.weddingevent.common.domain.ServicePackage;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.ServicePackageRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Service packages (feature spec §5.4). Staff can read (booking/lead forms); only admins manage. */
@RestController
@RequestMapping("/api/v1/admin/packages")
public class PackageController {

    public record PackageDto(String id, String name, String type, BigDecimal price, List<String> features, boolean isActive) {
        static PackageDto of(ServicePackage p) {
            return new PackageDto(p.getId().toString(), p.getName(), p.getType(), p.getPrice(), List.copyOf(p.getFeatures()), p.isActive());
        }
    }

    public record CreatePackageRequest(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 100) String type,
            @NotNull @Positive BigDecimal price,
            @Size(max = 20) List<@NotBlank @Size(max = 255) String> features) {}

    public record SetActiveRequest(@NotNull Boolean isActive) {}

    private final ServicePackageRepository packages;

    public PackageController(ServicePackageRepository packages) {
        this.packages = packages;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<List<PackageDto>> list() {
        return ApiResponse.ok(packages.findAllByVendorIdIsNullOrderByPriceDesc().stream().map(PackageDto::of).toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    @Transactional
    public ApiResponse<PackageDto> create(@Valid @RequestBody CreatePackageRequest req) {
        ServicePackage p = new ServicePackage();
        p.setName(req.name().trim());
        p.setType(req.type().trim());
        p.setPrice(req.price());
        if (req.features() != null) {
            req.features().stream().map(String::trim).forEach(p.getFeatures()::add);
        }
        return ApiResponse.ok(PackageDto.of(packages.save(p)), "Đã tạo gói dịch vụ");
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize(ADMIN)
    @Transactional
    public ApiResponse<PackageDto> setActive(@PathVariable UUID id, @Valid @RequestBody SetActiveRequest req) {
        ServicePackage p = packages.findById(id).orElseThrow(() -> new ResourceNotFoundException("gói dịch vụ", id));
        p.setActive(req.isActive());
        return ApiResponse.ok(PackageDto.of(p));
    }
}
