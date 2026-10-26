package com.weddingevent.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Pending e-mail verification code of a mobile user (FN-AUTH-02). Only the hash is stored; it proves {@code address} only. */
@Getter
@Setter
@Entity
@Table(name = "email_otps")
public class EmailOtp extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    /** Where the code was sent: the account's e-mail, or the new address it is changing to */
    @Column(nullable = false)
    private String address;

    /** Codes sent since {@code sendWindowStart}; capped per hour (FN-AUTH-02) */
    @Column(name = "send_count", nullable = false)
    private int sendCount;

    @Column(name = "send_window_start", nullable = false)
    private Instant sendWindowStart;
}
