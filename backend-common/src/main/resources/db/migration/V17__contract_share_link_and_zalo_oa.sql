-- FN-PUB-CONTR-01: public contract link sent over Zalo. Only the SHA-256 of the token is stored; the customer's
-- "Tôi đồng ý" is recorded once with IP and user agent.
ALTER TABLE contracts
    ADD COLUMN public_token_hash   VARCHAR(64) UNIQUE,
    ADD COLUMN share_expires_at    TIMESTAMPTZ,
    ADD COLUMN accepted_at         TIMESTAMPTZ,
    ADD COLUMN accepted_ip         VARCHAR(45),
    ADD COLUMN accepted_user_agent VARCHAR(500);

-- FN-ADM-CONTR-01 endpoint 3 / FN-ADM-SET-02: the studio's own Zalo Official Account sends the ZNS. The refresh token
-- is single-use, so the latest one is always stored back here.
ALTER TABLE studio_settings
    ADD COLUMN zalo_oa_app_id                  VARCHAR(50),
    ADD COLUMN zalo_oa_app_secret              VARCHAR(255),
    ADD COLUMN zalo_oa_refresh_token           TEXT,
    ADD COLUMN zalo_oa_access_token            TEXT,
    ADD COLUMN zalo_oa_access_token_expires_at TIMESTAMPTZ,
    ADD COLUMN zns_contract_template_id        VARCHAR(50);
