-- FN-VND-03/04 (v1.1.3): an accepted quote request becomes the vendor's project. cancel_* = the couple asked to
-- cancel and the vendor hasn't decided yet (FN-VND-04); the status stays unchanged meanwhile.
CREATE TABLE vendor_projects (
    id                  UUID PRIMARY KEY,
    quote_request_id    UUID           NOT NULL UNIQUE REFERENCES quote_requests (id) ON DELETE CASCADE,
    vendor_profile_id   UUID           NOT NULL REFERENCES vendor_profiles (id) ON DELETE CASCADE,
    couple_user_id      UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    service_package_id  UUID           NOT NULL REFERENCES service_packages (id),
    package_name        VARCHAR(255)   NOT NULL,
    couple_name         VARCHAR(100)   NOT NULL,
    event_date          DATE           NOT NULL,
    venue               VARCHAR(255),
    agreed_price        NUMERIC(15, 0) NOT NULL CHECK (agreed_price > 0),
    status              VARCHAR(20)    NOT NULL,
    cancel_requested_at TIMESTAMPTZ,
    cancel_reason       VARCHAR(500),
    version             BIGINT         NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ    NOT NULL,
    updated_at          TIMESTAMPTZ    NOT NULL
);
CREATE INDEX idx_vendor_projects_vendor ON vendor_projects (vendor_profile_id, status, event_date);
CREATE INDEX idx_vendor_projects_couple ON vendor_projects (couple_user_id, event_date);
CREATE INDEX idx_vendor_projects_package ON vendor_projects (service_package_id);
