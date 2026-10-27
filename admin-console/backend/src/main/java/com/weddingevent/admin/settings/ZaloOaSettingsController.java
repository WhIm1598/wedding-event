package com.weddingevent.admin.settings;

import static com.weddingevent.admin.security.SecurityConfig.ADMIN;

import com.weddingevent.admin.security.CurrentStudio;
import com.weddingevent.common.api.ApiResponse;
import com.weddingevent.common.domain.StudioSettings;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.StudioSettingsRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FN-ADM-SET-02: the studio's own Zalo Official Account used for ZNS. The app secret and refresh token are write-only:
 * reads only say whether they are set.
 */
@RestController
@RequestMapping("/api/v1/admin/settings/zalo-oa")
@PreAuthorize(ADMIN)
public class ZaloOaSettingsController {

    /** {@code accessTokenExpiresAt}: when the last token from Zalo runs out (null until the first message) */
    public record ZaloOaSettingsDto(String appId, String templateId, boolean hasAppSecret, boolean hasRefreshToken,
            boolean configured, Instant accessTokenExpiresAt) {
        static ZaloOaSettingsDto of(StudioSettings s) {
            return new ZaloOaSettingsDto(s.getZaloOaAppId(), s.getZnsContractTemplateId(), s.getZaloOaAppSecret() != null,
                    s.getZaloOaRefreshToken() != null, s.isZaloOaConfigured(), s.getZaloOaAccessTokenExpiresAt());
        }
    }

    /** {@code appSecret} / {@code refreshToken}: null or blank keeps the stored value */
    public record UpdateZaloOaRequest(
            @NotBlank @Size(max = 50) String appId,
            @Size(max = 255) String appSecret,
            @Size(max = 2000) String refreshToken,
            @NotBlank @Size(max = 50) String templateId) {}

    private final StudioSettingsRepository settings;

    public ZaloOaSettingsController(StudioSettingsRepository settings) {
        this.settings = settings;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<ZaloOaSettingsDto> get() {
        return ApiResponse.ok(ZaloOaSettingsDto.of(load(false)));
    }

    /** A new refresh token or app drops the cached access token, so the next message signs in with the new one */
    @PutMapping
    @Transactional
    public ApiResponse<ZaloOaSettingsDto> update(@Valid @RequestBody UpdateZaloOaRequest req) {
        StudioSettings s = load(true);
        boolean newCredentials = !req.appId().trim().equals(s.getZaloOaAppId()) || notBlank(req.appSecret()) || notBlank(req.refreshToken());
        s.setZaloOaAppId(req.appId().trim());
        s.setZnsContractTemplateId(req.templateId().trim());
        if (notBlank(req.appSecret())) {
            s.setZaloOaAppSecret(req.appSecret().trim());
        }
        if (notBlank(req.refreshToken())) {
            s.setZaloOaRefreshToken(req.refreshToken().trim());
        }
        if (newCredentials) {
            s.setZaloOaAccessToken(null);
            s.setZaloOaAccessTokenExpiresAt(null);
        }
        return ApiResponse.ok(ZaloOaSettingsDto.of(s), "Đã lưu cấu hình Zalo OA");
    }

    /** The row lock keeps a save from overwriting a refresh token that a message send is rotating at the same moment */
    private StudioSettings load(boolean forUpdate) {
        UUID studio = CurrentStudio.require();
        return (forUpdate ? settings.findByIdForUpdate(studio) : settings.findById(studio))
                .orElseThrow(() -> new ResourceNotFoundException("cấu hình studio", studio));
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
