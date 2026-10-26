package com.weddingevent.client.user;

import com.weddingevent.client.auth.OtpService;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.VendorProfile;
import com.weddingevent.common.domain.WeddingPlan;
import com.weddingevent.common.domain.enums.PlanStatus;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.repository.UserRepository;
import com.weddingevent.common.repository.VendorProfileRepository;
import com.weddingevent.common.repository.WeddingPlanRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Current user, role picker (FN-AUTH-03) and profile (FN-AUTH-04). */
@Service
public class UserService {

    public static final String VN_PHONE = "^(03|05|07|08|09)\\d{8}$";

    public record RoleRequest(
            @NotBlank(message = "Vui lòng chọn vai trò")
            @Pattern(regexp = "ROLE_COUPLE|ROLE_VENDOR", message = "Vai trò chỉ có thể là ROLE_COUPLE hoặc ROLE_VENDOR")
            String role) {}

    public record ProfileRequest(
            @NotBlank(message = "Họ tên không được để trống") @Size(max = 100, message = "Họ tên tối đa 100 ký tự") String fullName,
            @NotBlank(message = "Email không được để trống") @Email(message = "Email không hợp lệ") @Size(max = 255) String email,
            @Pattern(regexp = "^$|" + VN_PHONE, message = "Số điện thoại không hợp lệ") String phone) {}

    private final UserRepository users;
    private final WeddingPlanRepository plans;
    private final VendorProfileRepository vendors;
    private final OtpService otp;

    public UserService(UserRepository users, WeddingPlanRepository plans, VendorProfileRepository vendors, OtpService otp) {
        this.users = users;
        this.plans = plans;
        this.vendors = vendors;
        this.otp = otp;
    }

    @Transactional(readOnly = true)
    public UserDto me(UUID userId) {
        return dto(load(userId));
    }

    /** DTO with {@code hasPlan} derived from wedding_plans (feature spec §6.2: not stored) */
    @Transactional(readOnly = true)
    public UserDto dto(User user) {
        return UserDto.of(user, plans.existsActiveFor(user.getId()));
    }

    /**
     * FN-AUTH-03. The role is chosen once (409 ROLE_ALREADY_SET afterwards); couples get a DRAFT plan, vendors a
     * default vendor profile.
     */
    @Transactional
    public UserDto selectRole(UUID userId, RoleRequest req) {
        User user = load(userId);
        if (user.getRole() != null) {
            throw AppException.conflict("ROLE_ALREADY_SET", "Bạn đã chọn vai trò cho tài khoản này");
        }
        Role role = Role.valueOf(req.role());
        user.setRole(role);
        if (role == Role.ROLE_COUPLE) {
            if (plans.findByOwnerIdAndStatus(userId, PlanStatus.DRAFT).isEmpty() && !plans.existsActiveFor(userId)) {
                WeddingPlan draft = new WeddingPlan();
                draft.setOwner(user);
                draft.setStatus(PlanStatus.DRAFT);
                plans.save(draft);
            }
        } else if (vendors.findByUserId(userId).isEmpty()) {
            VendorProfile profile = new VendorProfile();
            profile.setUser(user);
            profile.setBusinessName(user.getFullName());
            vendors.save(profile);
        }
        return dto(user);
    }

    /**
     * FN-AUTH-04. A new e-mail is only kept as {@code pendingEmail} and an OTP is sent to it; it replaces the current
     * one when that OTP is confirmed (FN-AUTH-02). Until then the account keeps its proven address, so nobody can
     * claim an address they don't own. Sending the current e-mail again cancels a pending change.
     */
    @Transactional
    public UserDto updateProfile(UUID userId, ProfileRequest req) {
        User user = load(userId);
        String email = req.email().trim();
        boolean emailChanged = !email.equalsIgnoreCase(user.getEmail());
        if (emailChanged && users.existsByEmailIgnoreCase(email)) {
            throw AppException.conflict("EMAIL_EXISTS", "Email đã được sử dụng bởi tài khoản khác");
        }
        user.setFullName(req.fullName().trim());
        user.setPhone(req.phone() == null || req.phone().isBlank() ? null : req.phone());
        if (!emailChanged) {
            user.setPendingEmail(null);
        } else if (!email.equalsIgnoreCase(user.getPendingEmail())) {
            user.setPendingEmail(email);
            otp.issue(user, email);
        }
        return dto(user);
    }

    private User load(UUID userId) {
        return users.findById(userId).orElseThrow(() -> AppException.unauthorized("UNAUTHORIZED", "Phiên đăng nhập không hợp lệ"));
    }
}
