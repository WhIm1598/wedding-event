package com.weddingevent.admin.zalo;

import com.weddingevent.admin.zalo.ZaloOaClient.Tokens;
import com.weddingevent.admin.zalo.ZaloOaClient.ZaloException;
import com.weddingevent.common.domain.StudioSettings;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.repository.StudioSettingsRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The studio's Zalo OA access token. Each refresh consumes the stored refresh token and returns a new one, so it runs in
 * its own transaction (kept even if the message send that needed it fails) under a lock on the settings row.
 */
@Service
public class ZaloOaTokens {

    /** Refresh this long before Zalo's expiry so a token can't run out mid-request */
    static final Duration EARLY_REFRESH = Duration.ofMinutes(10);

    private final StudioSettingsRepository settings;
    private final ZaloOaClient client;
    private final Clock clock;

    public ZaloOaTokens(StudioSettingsRepository settings, ZaloOaClient client, Clock clock) {
        this.settings = settings;
        this.client = client;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String accessToken() {
        StudioSettings s = settings.findByIdForUpdate(StudioSettings.SINGLETON_ID).filter(StudioSettings::isZaloOaConfigured)
                .orElseThrow(ZaloOaTokens::notConfigured);
        Instant now = clock.instant();
        if (s.getZaloOaAccessToken() != null && s.getZaloOaAccessTokenExpiresAt() != null
                && s.getZaloOaAccessTokenExpiresAt().isAfter(now.plus(EARLY_REFRESH))) {
            return s.getZaloOaAccessToken();
        }
        Tokens tokens;
        try {
            tokens = client.refresh(s.getZaloOaAppId(), s.getZaloOaAppSecret(), s.getZaloOaRefreshToken());
        } catch (ZaloException e) {
            throw new AppException(HttpStatus.BAD_GATEWAY, "ZALO_OA_AUTH_FAILED",
                    "Không kết nối được Zalo OA của studio. Quản lý cần cập nhật refresh token trong Cài đặt → Zalo OA");
        }
        s.setZaloOaAccessToken(tokens.accessToken());
        s.setZaloOaRefreshToken(tokens.refreshToken());
        s.setZaloOaAccessTokenExpiresAt(now.plus(tokens.expiresIn()));
        return tokens.accessToken();
    }

    /** After Zalo rejected the access token: the next call refreshes */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void invalidate() {
        settings.findByIdForUpdate(StudioSettings.SINGLETON_ID).ifPresent(s -> s.setZaloOaAccessTokenExpiresAt(null));
    }

    public static AppException notConfigured() {
        return AppException.badRequest("ZALO_OA_NOT_CONFIGURED", "Studio chưa kết nối Zalo OA (Cài đặt → Zalo OA)");
    }
}
