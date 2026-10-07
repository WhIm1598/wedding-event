package com.weddingevent.client.auth;

import com.weddingevent.client.auth.GoogleIdTokenVerifier.GoogleProfile;
import com.weddingevent.client.security.TokenService;
import com.weddingevent.client.security.TokenService.TokenPair;
import com.weddingevent.client.security.UserAuthenticationConverter;
import com.weddingevent.client.user.UserDto;
import com.weddingevent.client.user.UserService;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.repository.UserRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FN-AUTH-01 (e-mail login), Google login (feature spec §7.1) and FN-AUTH-02 (OTP). */
@Service
public class AuthService {

    public record LoginRequest(
            @NotBlank(message = "Email không được để trống") @Email(message = "Email không hợp lệ") String email,
            @NotBlank(message = "Mật khẩu không được để trống") @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự") String password) {}

    public record GoogleLoginRequest(@NotBlank(message = "Thiếu Google ID token") String idToken) {}

    public record SendOtpRequest(@NotBlank @Email(message = "Email không hợp lệ") String email) {}

    public record VerifyOtpRequest(
            @NotBlank @Email(message = "Email không hợp lệ") String email,
            @NotBlank(message = "Vui lòng nhập mã OTP") @Pattern(regexp = "^[0-9]{4}$", message = "Mã OTP gồm đúng 4 chữ số") String otpCode) {}

    public record LoginResponse(String accessToken, String refreshToken, UserDto user) {}

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final OtpService otp;
    private final GoogleIdTokenVerifier google;
    private final UserService userService;
    /** Compared against when the email is unknown, so response time doesn't reveal which emails exist */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokens, OtpService otp,
            GoogleIdTokenVerifier google, UserService userService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.otp = otp;
        this.google = google;
        this.userService = userService;
        this.dummyHash = passwordEncoder.encode("timing-equalizer");
    }

    /**
     * FN-AUTH-01: same 401 INVALID_CREDENTIALS for unknown e-mail and wrong password; studio accounts (admin/staff)
     * are treated as unknown here — they sign in to the admin console. Unverified accounts get a new OTP but still
     * receive tokens (step 4); the app keeps the user on the OTP screen.
     */
    @Transactional
    public LoginResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email().trim()).orElse(null);
        if (user == null) {
            passwordEncoder.matches(req.password(), dummyHash);
            throw invalidCredentials();
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash()) || !UserAuthenticationConverter.isMobileUser(user)) {
            throw invalidCredentials();
        }
        return signIn(user);
    }

    /** Creates the account on first use (no role yet), or signs in the account with the same e-mail. */
    @Transactional
    public LoginResponse loginWithGoogle(GoogleLoginRequest req) {
        GoogleProfile profile = google.verify(req.idToken());
        User user = users.findByEmailIgnoreCase(profile.email().trim()).orElse(null);
        if (user == null) {
            user = new User();
            user.setEmail(profile.email().trim().toLowerCase(Locale.ROOT));
            user.setFullName(profile.name() == null || profile.name().isBlank()
                    ? user.getEmail().substring(0, user.getEmail().indexOf('@'))
                    : profile.name().trim());
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString())); // unusable for e-mail login
            user = users.save(user);
        } else if (!UserAuthenticationConverter.isMobileUser(user)) {
            throw invalidCredentials();
        }
        if (profile.emailVerified()) {
            user.setVerified(true);
        }
        if (user.getAvatarUrl() == null) {
            user.setAvatarUrl(profile.pictureUrl());
        }
        return signIn(user);
    }

    @Transactional(readOnly = true)
    public UserDto me(UUID userId) {
        return userService.me(userId);
    }

    private LoginResponse signIn(User user) {
        if (!user.isActive()) {
            throw AppException.forbidden("ACCOUNT_LOCKED", "Tài khoản đã bị khóa, vui lòng liên hệ hỗ trợ");
        }
        if (!user.isVerified()) {
            otp.issue(user);
        }
        TokenPair pair = tokens.issue(user);
        return new LoginResponse(pair.accessToken(), pair.refreshToken(), userService.dto(user));
    }

    private static AppException invalidCredentials() {
        return AppException.unauthorized("INVALID_CREDENTIALS", "Email hoặc mật khẩu không chính xác");
    }
}
