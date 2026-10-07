package com.weddingevent.client.auth;

import com.weddingevent.client.auth.AuthService.GoogleLoginRequest;
import com.weddingevent.client.auth.AuthService.LoginRequest;
import com.weddingevent.client.auth.AuthService.LoginResponse;
import com.weddingevent.client.auth.AuthService.SendOtpRequest;
import com.weddingevent.client.auth.AuthService.VerifyOtpRequest;
import com.weddingevent.client.security.CurrentUser;
import com.weddingevent.client.user.UserDto;
import com.weddingevent.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;
    private final OtpService otp;

    public AuthController(AuthService auth, OtpService otp) {
        this.auth = auth;
        this.otp = otp;
    }

    /** FN-AUTH-01 */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(auth.login(req), "Đăng nhập thành công");
    }

    @PostMapping("/google")
    public ApiResponse<LoginResponse> google(@Valid @RequestBody GoogleLoginRequest req) {
        return ApiResponse.ok(auth.loginWithGoogle(req), "Đăng nhập thành công");
    }

    /** Always 200: the answer must not reveal whether the e-mail has an account */
    @PostMapping("/send-otp")
    public ApiResponse<Void> sendOtp(@Valid @RequestBody SendOtpRequest req) {
        otp.resend(req.email());
        return ApiResponse.ok(null, "Nếu email hợp lệ, mã OTP đã được gửi");
    }

    /** FN-AUTH-02 */
    @PostMapping("/verify-otp")
    public ApiResponse<Void> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        otp.verify(req.email(), req.otpCode());
        return ApiResponse.ok(null, "Xác thực email thành công");
    }

    @GetMapping("/me")
    public ApiResponse<UserDto> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(auth.me(CurrentUser.id(jwt)));
    }
}
