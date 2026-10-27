-- ADR-0001: many studios share one database. Every studio row gets studio_id; the data that exists today becomes
-- the first studio (V1 always created its settings row, so there is always exactly one to migrate).

CREATE TABLE studios (
    id                      UUID PRIMARY KEY,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    subscription_expires_at DATE,
    created_at              TIMESTAMPTZ NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL
);

INSERT INTO studios (id, status, created_at, updated_at) VALUES (gen_random_uuid(), 'ACTIVE', now(), now());

-- Studio-only tables: filtered by Hibernate @TenantId in the admin backend
DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['staff_members', 'staff_tasks', 'bookings', 'assets', 'asset_reservations', 'leads',
                             'contracts', 'payment_transactions']
    LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN studio_id UUID REFERENCES studios (id)', t);
        EXECUTE format('UPDATE %I SET studio_id = (SELECT id FROM studios)', t);
        EXECUTE format('ALTER TABLE %I ALTER COLUMN studio_id SET NOT NULL', t);
        EXECUTE format('CREATE INDEX idx_%s_studio ON %I (studio_id)', t, t);
    END LOOP;
END $$;

-- Codes are numbered per studio
ALTER TABLE staff_members DROP CONSTRAINT staff_members_code_key, ADD CONSTRAINT uq_staff_members_code UNIQUE (studio_id, code);
ALTER TABLE assets DROP CONSTRAINT assets_code_key, ADD CONSTRAINT uq_assets_code UNIQUE (studio_id, code);
ALTER TABLE contracts DROP CONSTRAINT contracts_contract_number_key, ADD CONSTRAINT uq_contracts_number UNIQUE (studio_id, contract_number);
ALTER TABLE payment_transactions DROP CONSTRAINT payment_transactions_code_key, ADD CONSTRAINT uq_payment_transactions_code UNIQUE (studio_id, code);

-- Tables shared with the mobile app: studio_id only on the studio's rows, filtered by hand
ALTER TABLE users ADD COLUMN studio_id UUID REFERENCES studios (id);
UPDATE users SET studio_id = (SELECT id FROM studios) WHERE role IN ('ROLE_ADMIN', 'ROLE_STAFF');
ALTER TABLE users ADD CONSTRAINT ck_users_console_studio CHECK (role NOT IN ('ROLE_ADMIN', 'ROLE_STAFF') OR studio_id IS NOT NULL);
CREATE INDEX idx_users_studio ON users (studio_id);

ALTER TABLE service_packages ADD COLUMN studio_id UUID REFERENCES studios (id);
UPDATE service_packages SET studio_id = (SELECT id FROM studios) WHERE vendor_id IS NULL;
ALTER TABLE service_packages ADD CONSTRAINT ck_service_packages_owner CHECK ((vendor_id IS NULL) <> (studio_id IS NULL));
CREATE INDEX idx_service_packages_studio ON service_packages (studio_id);

ALTER TABLE notifications ADD COLUMN studio_id UUID REFERENCES studios (id);
UPDATE notifications SET studio_id = (SELECT id FROM studios) WHERE recipient_user_id IS NULL;
CREATE INDEX idx_notifications_studio ON notifications (studio_id, created_at);

-- One settings row per studio
ALTER TABLE studio_settings ADD COLUMN studio_id UUID REFERENCES studios (id);
UPDATE studio_settings SET studio_id = (SELECT id FROM studios);
ALTER TABLE studio_settings DROP CONSTRAINT studio_settings_pkey;
ALTER TABLE studio_settings DROP COLUMN id;
ALTER TABLE studio_settings ALTER COLUMN studio_id SET NOT NULL;
ALTER TABLE studio_settings ADD PRIMARY KEY (studio_id);

-- Per-studio counters replace the global sequences; the first studio continues where they stopped
CREATE TABLE studio_counters (
    studio_id  UUID        NOT NULL REFERENCES studios (id),
    kind       VARCHAR(20) NOT NULL,
    last_value BIGINT      NOT NULL,
    PRIMARY KEY (studio_id, kind)
);
INSERT INTO studio_counters (studio_id, kind, last_value)
SELECT (SELECT id FROM studios), 'STAFF', CASE WHEN is_called THEN last_value ELSE last_value - 1 END FROM staff_code_seq
UNION ALL
SELECT (SELECT id FROM studios), 'CONTRACT', CASE WHEN is_called THEN last_value ELSE last_value - 1 END FROM contract_number_seq
UNION ALL
SELECT (SELECT id FROM studios), 'TRANSACTION', CASE WHEN is_called THEN last_value ELSE last_value - 1 END FROM transaction_code_seq;
DROP SEQUENCE staff_code_seq, contract_number_seq, transaction_code_seq;
