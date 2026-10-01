-- Lumière Studios admin schema (PostgreSQL 16). Mirrors backend-common JPA entities;
-- Hibernate runs with ddl-auto=validate so any drift fails fast at startup.

CREATE SEQUENCE staff_code_seq START 1;
CREATE SEQUENCE contract_number_seq START 102;
CREATE SEQUENCE transaction_code_seq START 1;

CREATE TABLE staff_members (
    id          UUID PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    full_name   VARCHAR(100) NOT NULL,
    position    VARCHAR(100) NOT NULL,
    phone       VARCHAR(20)  NOT NULL,
    work_status VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(100) NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    avatar_url      VARCHAR(500),
    role            VARCHAR(20)  NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    is_verified     BOOLEAN      NOT NULL DEFAULT FALSE,
    staff_member_id UUID REFERENCES staff_members (id),
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);

CREATE TABLE staff_tasks (
    id              UUID PRIMARY KEY,
    staff_member_id UUID         NOT NULL REFERENCES staff_members (id) ON DELETE CASCADE,
    title           VARCHAR(255) NOT NULL,
    due_date        DATE         NOT NULL,
    notes           VARCHAR(1000),
    completed       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_staff_tasks_staff ON staff_tasks (staff_member_id);

CREATE TABLE service_packages (
    id            UUID PRIMARY KEY,
    vendor_id     UUID,
    name          VARCHAR(200)   NOT NULL,
    type          VARCHAR(100)   NOT NULL,
    price         NUMERIC(15, 0) NOT NULL CHECK (price >= 0),
    description   VARCHAR(2000),
    thumbnail_url VARCHAR(500),
    is_active     BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ    NOT NULL,
    updated_at    TIMESTAMPTZ    NOT NULL
);

CREATE TABLE service_package_features (
    package_id UUID         NOT NULL REFERENCES service_packages (id) ON DELETE CASCADE,
    position   INTEGER      NOT NULL,
    feature    VARCHAR(255) NOT NULL,
    PRIMARY KEY (package_id, position)
);

CREATE TABLE bookings (
    id                 UUID PRIMARY KEY,
    customer_name      VARCHAR(100) NOT NULL,
    phone              VARCHAR(20),
    booking_type       VARCHAR(100) NOT NULL,
    service_package_id UUID REFERENCES service_packages (id),
    event_date         DATE         NOT NULL,
    event_time         TIME         NOT NULL,
    venue_address      VARCHAR(255),
    status             VARCHAR(20)  NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_bookings_event_date ON bookings (event_date);

CREATE TABLE booking_staff (
    booking_id      UUID NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    staff_member_id UUID NOT NULL REFERENCES staff_members (id),
    PRIMARY KEY (booking_id, staff_member_id)
);

CREATE TABLE assets (
    id                      UUID PRIMARY KEY,
    code                    VARCHAR(20)  NOT NULL UNIQUE,
    name                    VARCHAR(200) NOT NULL,
    category                VARCHAR(30)  NOT NULL,
    size                    VARCHAR(20),
    status                  VARCHAR(20)  NOT NULL,
    maintenance_buffer_days INTEGER      NOT NULL DEFAULT 0 CHECK (maintenance_buffer_days >= 0),
    created_at              TIMESTAMPTZ  NOT NULL,
    updated_at              TIMESTAMPTZ  NOT NULL
);

CREATE TABLE asset_reservations (
    id            UUID PRIMARY KEY,
    asset_id      UUID        NOT NULL REFERENCES assets (id),
    booking_id    UUID REFERENCES bookings (id),
    start_date    DATE        NOT NULL,
    end_date      DATE        NOT NULL,
    lock_end_date DATE        NOT NULL,
    status        VARCHAR(20) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    CHECK (end_date >= start_date AND lock_end_date >= end_date)
);
CREATE INDEX idx_asset_reservations_asset_dates ON asset_reservations (asset_id, start_date, lock_end_date);

CREATE TABLE leads (
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    has_zalo   BOOLEAN      NOT NULL DEFAULT FALSE,
    interest   VARCHAR(200),
    stage      VARCHAR(30)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_leads_phone ON leads (phone);

CREATE TABLE contracts (
    id                 UUID PRIMARY KEY,
    contract_number    VARCHAR(20)    NOT NULL UNIQUE,
    lead_id            UUID REFERENCES leads (id),
    booking_id         UUID REFERENCES bookings (id),
    customer_name      VARCHAR(100)   NOT NULL,
    phone              VARCHAR(20)    NOT NULL,
    has_zalo           BOOLEAN        NOT NULL DEFAULT FALSE,
    service_package_id UUID REFERENCES service_packages (id),
    package_name       VARCHAR(200)   NOT NULL,
    total_amount       NUMERIC(15, 0) NOT NULL CHECK (total_amount > 0),
    paid_amount        NUMERIC(15, 0) NOT NULL DEFAULT 0 CHECK (paid_amount >= 0),
    remaining_amount   NUMERIC(15, 0) NOT NULL CHECK (remaining_amount >= 0),
    contract_date      DATE           NOT NULL,
    pdf_file_url       VARCHAR(500),
    status             VARCHAR(20)    NOT NULL,
    notes              VARCHAR(1000),
    version            BIGINT         NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ    NOT NULL,
    updated_at         TIMESTAMPTZ    NOT NULL
);

CREATE TABLE payment_transactions (
    id               UUID PRIMARY KEY,
    code             VARCHAR(20)    NOT NULL UNIQUE,
    contract_id      UUID REFERENCES contracts (id),
    amount           NUMERIC(15, 0) NOT NULL CHECK (amount > 0),
    type             VARCHAR(10)    NOT NULL,
    category         VARCHAR(100)   NOT NULL,
    description      VARCHAR(255)   NOT NULL,
    transaction_date DATE           NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL,
    updated_at       TIMESTAMPTZ    NOT NULL
);
CREATE INDEX idx_transactions_type_date ON payment_transactions (type, transaction_date);

CREATE TABLE notifications (
    id          UUID PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    description VARCHAR(500) NOT NULL,
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE studio_settings (
    id                   SMALLINT PRIMARY KEY CHECK (id = 1),
    name                 VARCHAR(200) NOT NULL,
    address              VARCHAR(255) NOT NULL,
    tax_code             VARCHAR(20)  NOT NULL,
    legal_representative VARCHAR(100) NOT NULL,
    bank_info            VARCHAR(500) NOT NULL
);

INSERT INTO studio_settings (id, name, address, tax_code, legal_representative, bank_info)
VALUES (1, 'LUMIÈRE STUDIOS', 'Cập nhật địa chỉ trong Cài đặt', 'Chưa cập nhật', 'Chưa cập nhật', 'Chưa cập nhật');
