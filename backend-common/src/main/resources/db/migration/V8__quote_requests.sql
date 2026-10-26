-- FN-VND-01..03 (v1.1.3): a couple asks a vendor to quote one of its service packages.
CREATE TABLE quote_requests (
    id                 UUID PRIMARY KEY,
    couple_user_id     UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    vendor_profile_id  UUID           NOT NULL REFERENCES vendor_profiles (id) ON DELETE CASCADE,
    service_package_id UUID           NOT NULL REFERENCES service_packages (id),
    package_name       VARCHAR(255)   NOT NULL,
    event_date         DATE,
    venue              VARCHAR(255),
    guest_count        INTEGER CHECK (guest_count BETWEEN 1 AND 5000),
    message            VARCHAR(1000),
    status             VARCHAR(20)    NOT NULL,
    quoted_price       NUMERIC(15, 0) CHECK (quoted_price > 0),
    vendor_note        VARCHAR(1000),
    responded_at       TIMESTAMPTZ,
    version            BIGINT         NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ    NOT NULL,
    updated_at         TIMESTAMPTZ    NOT NULL
);
CREATE INDEX idx_quote_requests_couple ON quote_requests (couple_user_id, created_at DESC);
CREATE INDEX idx_quote_requests_vendor ON quote_requests (vendor_profile_id, created_at DESC);
-- QUOTE_ALREADY_OPEN: at most one open request per couple and package, also under concurrent sends
CREATE UNIQUE INDEX uq_quote_requests_open ON quote_requests (couple_user_id, service_package_id)
    WHERE status IN ('PENDING', 'QUOTED');
