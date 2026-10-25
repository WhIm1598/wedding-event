package com.weddingevent.client.auth;

import com.weddingevent.client.config.AppProperties;
import com.weddingevent.common.domain.EmailOtp;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.repository.EmailOtpRepository;
import com.weddingevent.common.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FN-AUTH-02: 4-digit codes, valid {@code app.otp.ttl} (5 min), disabled after {@code max-attempts} (5) wrong tries,
 * at most {@value #MAX_SENDS_PER_WINDOW} codes per account per hour. A code proves only the address it was sent to:
 * the account's e-mail (verifies it) or its pending new e-mail (FN-AUTH-04: replaces the old one).
 */
@Service
public class OtpService {

    /** Each new code resets the wrong-attempt counter, so this caps guesses at 25 per hour (10 000 codes) */
    static final int MAX_SENDS_PER_WINDOW = 5;
    private static final Duration SEND_WINDOW = Duration.ofHours(1);

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

    /**
     * Replaces any pending code of the user with a new one for {@code address} and sends it. Only the hash is stored.
     * Over the hourly limit nothing is sent (silently: the callers must not reveal it) and the old code stays.
     */
    @Transactional
    public void issue(User user, String address) {
        Instant now = clock.instant();
        EmailOtp otp = otps.findByUserId(user.getId()).orElseGet(() -> {
            EmailOtp created = new EmailOtp();
            created.setUser(user);
            created.setSendWindowStart(now);
            return created;
        });
        if (otp.getSendWindowStart().plus(SEND_WINDOW).isBefore(now)) {
            otp.setSendWindowStart(now);
            otp.setSendCount(0);
        }
        if (otp.getSendCount() >= MAX_SENDS_PER_WINDOW) {
            return;
        }
        String code = String.format("%04d", random.nextInt(10_000));
        otp.setAddress(address);
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setExpiresAt(now.plus(props.otp().ttl()));
        otp.setFailedAttempts(0);
        otp.setSendCount(otp.getSendCount() + 1);
        otps.save(otp);
        sender.send(address, code);
    }

    /**
     * POST /auth/send-otp — for an unverified account with this e-mail, or an account changing to it. Silently does
     * nothing otherwise (no account probing).
     */
    @Transactional
    public void resend(String email) {
        String address = email.trim();
        users.findByEmailIgnoreCase(address)
                .filter(User::isActive)
                .filter(u -> !u.isVerified())
                .ifPresent(u -> issue(u, u.getEmail()));
        users.findByPendingEmailIgnoreCase(address).stream()
                .filter(User::isActive)
                .forEach(u -> issue(u, u.getPendingEmail()));
    }

    /**
     * Checks the code against every code sent to this address. Success verifies the account's e-mail, or swaps in
     * the pending one (409 EMAIL_EXISTS if another account took that address meanwhile). Wrong attempts are counted
     * even though the call fails, hence no rollback for business errors.
     */
    @Transactional(noRollbackFor = AppException.class)
    public void verify(String email, String code) {
        List<EmailOtp> sent = otps.findByAddressIgnoreCase(email.trim());
        if (sent.isEmpty()) {
            throw invalid();
        }
        Instant now = clock.instant();
        List<EmailOtp> live = sent.stream()
                .filter(o -> o.getFailedAttempts() < props.otp().maxAttempts() && o.getExpiresAt().isAfter(now))
                .toList();
        if (live.isEmpty()) {
            throw AppException.badRequest("OTP_EXPIRED", "Mã OTP đã hết hạn hoặc bị vô hiệu hóa, vui lòng yêu cầu gửi lại mã");
        }
        EmailOtp match = live.stream().filter(o -> passwordEncoder.matches(code, o.getCodeHash())).findFirst().orElse(null);
        if (match == null) {
            live.forEach(o -> o.setFailedAttempts(o.getFailedAttempts() + 1));
            throw invalid();
        }
        User user = match.getUser();
        otps.delete(match);
        if (match.getAddress().equalsIgnoreCase(user.getEmail())) {
            user.setVerified(true);
            return;
        }
        user.setPendingEmail(null); // this change can only succeed once, whatever happens below
        if (users.existsByEmailIgnoreCase(match.getAddress())) {
            throw AppException.conflict("EMAIL_EXISTS", "Email đã được sử dụng bởi tài khoản khác");
        }
        user.setEmail(match.getAddress());
        user.setVerified(true);
    }

    private static AppException invalid() {
        return AppException.badRequest("INVALID_OTP", "Mã OTP không chính xác");
    }
}
