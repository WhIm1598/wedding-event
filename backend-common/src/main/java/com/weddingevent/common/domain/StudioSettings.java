package com.weddingevent.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** One studio's profile printed on contracts (party B, bank details), keyed by its studio (ADR-0001). */
@Getter
@Setter
@Entity
@Table(name = "studio_settings")
public class StudioSettings {

    @Id
    @Column(name = "studio_id")
    private UUID studioId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(name = "tax_code", nullable = false)
    private String taxCode;

    @Column(name = "legal_representative", nullable = false)
    private String legalRepresentative;

    @Column(name = "bank_info", nullable = false)
    private String bankInfo;

    /** Zalo number couples open from the app's catalog (FN-SVC-02); null = no Zalo link */
    @Column(name = "zalo_phone")
    private String zaloPhone;

    // The studio's own Zalo Official Account for ZNS (FN-ADM-SET-02). Secrets are never returned by any API.
    @Column(name = "zalo_oa_app_id")
    private String zaloOaAppId;

    @Column(name = "zalo_oa_app_secret")
    private String zaloOaAppSecret;

    /** Single-use: every refresh returns a new one that must replace it */
    @Column(name = "zalo_oa_refresh_token")
    private String zaloOaRefreshToken;

    @Column(name = "zalo_oa_access_token")
    private String zaloOaAccessToken;

    @Column(name = "zalo_oa_access_token_expires_at")
    private Instant zaloOaAccessTokenExpiresAt;

    /** Approved ZNS template carrying the public contract link (params in docs/setup/zalo-oa-zns.md) */
    @Column(name = "zns_contract_template_id")
    private String znsContractTemplateId;

    public boolean isZaloOaConfigured() {
        return notBlank(zaloOaAppId) && notBlank(zaloOaAppSecret) && notBlank(zaloOaRefreshToken) && notBlank(znsContractTemplateId);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
