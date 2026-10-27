package com.weddingevent.common.domain;

import com.weddingevent.common.domain.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;

    @Column(name = "avatar_url")
    private String avatarUrl;

    /** Null for a new mobile account until it picks ROLE_COUPLE / ROLE_VENDOR (FN-AUTH-03). */
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    /** A new e-mail waiting for its OTP (FN-AUTH-04); {@code email} keeps the proven address until then */
    @Column(name = "pending_email")
    private String pendingEmail;

    /** Google's user id ({@code sub}) once the account has signed in with Google; null otherwise */
    @Column(name = "google_sub", unique = true)
    private String googleSub;

    /** Wrong admin-console passwords in a row (FN-ADM-AUTH-01 step 3) */
    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    /** Admin-console login refused until then */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    /** Set on a temporary password (FN-ADM-USER-01): only change-password, me and logout work until it is changed */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    /** Last successful admin-console login */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** Links a ROLE_STAFF account to its staff profile so they only see their own schedule. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_member_id")
    private StaffMember staffMember;
}
