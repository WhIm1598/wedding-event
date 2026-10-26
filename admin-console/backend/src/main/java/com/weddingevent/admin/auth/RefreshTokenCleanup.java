package com.weddingevent.admin.auth;

import com.weddingevent.common.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.Duration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Drops refresh token rows a week after they expired (kept that long for reuse detection). The mobile backend runs the
 * same delete on the shared table; this one keeps an admin-only deployment from growing it forever.
 */
@Component
public class RefreshTokenCleanup {

    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    public RefreshTokenCleanup(RefreshTokenRepository refreshTokens, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    @Scheduled(cron = "0 45 3 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public int run() {
        return refreshTokens.deleteExpiredBefore(clock.instant().minus(Duration.ofDays(7)));
    }
}
