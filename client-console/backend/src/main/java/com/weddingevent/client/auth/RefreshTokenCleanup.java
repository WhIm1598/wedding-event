package com.weddingevent.client.auth;

import com.weddingevent.common.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.Duration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** FN-AUTH-07: drops refresh token rows a week after they expired (kept that long for reuse detection) */
@Component
public class RefreshTokenCleanup {

    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    public RefreshTokenCleanup(RefreshTokenRepository refreshTokens, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public int run() {
        return refreshTokens.deleteExpiredBefore(clock.instant().minus(Duration.ofDays(7)));
    }
}
