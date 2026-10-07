package com.weddingevent.client.auth;

import com.weddingevent.client.config.AppProperties;
import com.weddingevent.common.domain.EmailOtp;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.repository.EmailOtpRepository;
import com.weddingevent.common.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Clock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FN-AUTH-02: 4-digit codes, valid {@code app.otp.ttl} (5 min), disabled after {@code max-attempts} (5) wrong tries. */
@Service
public class OtpService {

    private final EmailOtpRepository otps;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final OtpSender sender;
    private final AppProperties props;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public OtpService(EmailOtpRepository otps, UserRepository users, PasswordEncoder passwordEncoder, OtpSender sender,
            AppProperties props, Clock clock) {
        this.otps = otps;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.sender = sender;
        this.props = props;
        this.clock = clock;
    }

    /** Replaces any pending code of the user with a new one and sends it. Only the hash is stored. */
    @Transactional
    public void issue(User user) {
        String code = String.format("%04d", random.nextInt(10_000));
        EmailOtp otp = otps.findByUserId(user.getId()).orElseGet(() -> {
            EmailOtp created = new EmailOtp();
            created.setUser(user);
            return created;
        });
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setExpiresAt(clock.instant().plus(props.otp().ttl()));
        otp.setFailedAttempts(0);
        otps.save(otp);
        sender.send(user.getEmail(), code);
    }

    /** POST /auth/send-otp — silently does nothing for unknown or already verified addresses (no account probing) */
    @Transactional
    public void resend(String email) {
        users.findByEmailIgnoreCase(email.trim())
                .filter(User::isActive)
                .filter(u -> !u.isVerified())
                .ifPresent(this::issue);
    }

    /** Wrong attempts are counted even though the call fails, hence no rollback for business errors. */
    @Transactional(noRollbackFor = AppException.class)
    public void verify(String email, String code) {
        User user = users.findByEmailIgnoreCase(email.trim()).orElseThrow(OtpService::invalid);
        EmailOtp otp = otps.findByUserId(user.getId()).orElseThrow(OtpService::invalid);
        if (otp.getFailedAttempts() >= props.otp().maxAttempts() || !otp.getExpiresAt().isAfter(clock.instant())) {
            throw AppException.badRequest("OTP_EXPIRED", "Mã OTP đã hết hạn hoặc bị vô hiệu hóa, vui lòng yêu cầu gửi lại mã");
        }
        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            otp.setFailedAttempts(otp.getFailedAttempts() + 1);
            throw invalid();
        }
        user.setVerified(true);
        otps.delete(otp);
    }

    private static AppException invalid() {
        return AppException.badRequest("INVALID_OTP", "Mã OTP không chính xác");
    }
}
