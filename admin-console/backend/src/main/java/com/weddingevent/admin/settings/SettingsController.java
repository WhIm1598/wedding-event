package com.weddingevent.admin.settings;

import static com.weddingevent.admin.security.SecurityConfig.ADMIN;

import com.weddingevent.common.api.ApiResponse;
import com.weddingevent.common.domain.StudioSettings;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.StudioSettingsRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Studio profile printed as party B on contracts. Readable by staff (contract preview), editable by admins. */
@RestController
@RequestMapping("/api/v1/admin/settings/studio")
public class SettingsController {

    public record StudioSettingsDto(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 255) String address,
            @NotBlank @Size(max = 20) String taxCode,
            @NotBlank @Size(max = 100) String legalRepresentative,
            @NotBlank @Size(max = 500) String bankInfo) {

        public static StudioSettingsDto of(StudioSettings s) {
            return new StudioSettingsDto(s.getName(), s.getAddress(), s.getTaxCode(), s.getLegalRepresentative(), s.getBankInfo());
        }
    }

    private final StudioSettingsRepository settings;

    public SettingsController(StudioSettingsRepository settings) {
        this.settings = settings;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<StudioSettingsDto> get() {
        return ApiResponse.ok(StudioSettingsDto.of(load()));
    }

    @PutMapping
    @PreAuthorize(ADMIN)
    @Transactional
    public ApiResponse<StudioSettingsDto> update(@Valid @RequestBody StudioSettingsDto req) {
        StudioSettings s = load();
        s.setName(req.name().trim());
        s.setAddress(req.address().trim());
        s.setTaxCode(req.taxCode().trim());
        s.setLegalRepresentative(req.legalRepresentative().trim());
        s.setBankInfo(req.bankInfo().trim());
        return ApiResponse.ok(StudioSettingsDto.of(s), "Đã lưu cấu hình");
    }

    private StudioSettings load() {
        return settings.findById(StudioSettings.SINGLETON_ID)
                .orElseThrow(() -> new ResourceNotFoundException("cấu hình studio", StudioSettings.SINGLETON_ID));
    }
}
