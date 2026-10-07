-- WedPlanner mobile workspace, served by client-console/backend (detailed spec Part A).
-- Couples' wedding plans, budget, checklist, guests, partner sync codes, e-mail OTP, vendor profiles, chat,
-- and per-user in-app notifications. Backward compatible with the admin backend: only additions, plus one
-- relaxed constraint (users.role).

-- A mobile account picks ROLE_COUPLE / ROLE_VENDOR after signing up (FN-AUTH-03), so it has no role until then.
ALTER TABLE users ALTER COLUMN role DROP NOT NULL;

-- FN-AUTH-03: default vendor profile created when a user picks ROLE_VENDOR
CREATE TABLE vendor_profiles (
    id            UUID PRIMARY KEY,
    user_id       UUID          NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    business_name VARCHAR(200)  NOT NULL,
    address       VARCHAR(255),
    rating        NUMERIC(2, 1) NOT NULL DEFAULT 0 CHECK (rating >= 0 AND rating <= 5),
    review_count  INTEGER       NOT NULL DEFAULT 0 CHECK (review_count >= 0),
    created_at    TIMESTAMPTZ   NOT NULL,
    updated_at    TIMESTAMPTZ   NOT NULL
);

-- FN-SVC-01 catalog category (photo|decor|restaurant|dress). Studio packages are photography.
ALTER TABLE service_packages ADD COLUMN category VARCHAR(20) NOT NULL DEFAULT 'PHOTO';
ALTER TABLE service_packages
    ADD CONSTRAINT fk_service_packages_vendor FOREIGN KEY (vendor_id) REFERENCES vendor_profiles (id);
CREATE INDEX idx_service_packages_catalog ON service_packages (is_active, category);

-- FN-PLAN-01..05. DRAFT = created at role selection (no date/budget yet); PLANNING = the active plan.
CREATE TABLE wedding_plans (
    id                   UUID PRIMARY KEY,
    user_id              UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    partner_id           UUID REFERENCES users (id) ON DELETE SET NULL,
    wedding_date         DATE,
    engagement_date      DATE,
    total_budget         NUMERIC(15, 0) CHECK (total_budget >= 0),
    completed_percentage INTEGER     NOT NULL DEFAULT 0 CHECK (completed_percentage BETWEEN 0 AND 100),
    status               VARCHAR(20) NOT NULL,
    version              BIGINT      NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    CHECK (status = 'DRAFT' OR (wedding_date IS NOT NULL AND total_budget IS NOT NULL)),
    CHECK (partner_id IS NULL OR partner_id <> user_id)
);
-- One draft/active plan per owner, and a partner joins at most one active plan
CREATE UNIQUE INDEX uq_wedding_plans_owner_open ON wedding_plans (user_id) WHERE status IN ('DRAFT', 'PLANNING');
CREATE UNIQUE INDEX uq_wedding_plans_partner_active ON wedding_plans (partner_id) WHERE status = 'PLANNING' AND partner_id IS NOT NULL;

CREATE TABLE budget_items (
    id               UUID PRIMARY KEY,
    wedding_plan_id  UUID           NOT NULL REFERENCES wedding_plans (id) ON DELETE CASCADE,
    position         INTEGER        NOT NULL,
    category_name    VARCHAR(100)   NOT NULL,
    estimated_amount NUMERIC(15, 0) NOT NULL CHECK (estimated_amount >= 0),
    actual_amount    NUMERIC(15, 0) NOT NULL DEFAULT 0 CHECK (actual_amount >= 0),
    status           VARCHAR(20)    NOT NULL,
    notes            VARCHAR(500),
    created_at       TIMESTAMPTZ    NOT NULL,
    updated_at       TIMESTAMPTZ    NOT NULL
);
CREATE INDEX idx_budget_items_plan ON budget_items (wedding_plan_id);

CREATE TABLE plan_tasks (
    id              UUID PRIMARY KEY,
    wedding_plan_id UUID         NOT NULL REFERENCES wedding_plans (id) ON DELETE CASCADE,
    position        INTEGER      NOT NULL,
    title           VARCHAR(200) NOT NULL,
    due_date        DATE         NOT NULL,
    month_group     VARCHAR(50)  NOT NULL,
    is_completed    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_plan_tasks_plan ON plan_tasks (wedding_plan_id);

-- FN-GUEST-01..02
CREATE TABLE guests (
    id              UUID PRIMARY KEY,
    wedding_plan_id UUID         NOT NULL REFERENCES wedding_plans (id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    group_name      VARCHAR(100),
    status          VARCHAR(20)  NOT NULL,
    notes           VARCHAR(500),
    table_number    INTEGER CHECK (table_number > 0),
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_guests_plan ON guests (wedding_plan_id);

-- FN-PLAN-04: 6-character partner invitation codes, valid 24 hours, single use
CREATE TABLE plan_sync_codes (
    id              UUID PRIMARY KEY,
    wedding_plan_id UUID        NOT NULL REFERENCES wedding_plans (id) ON DELETE CASCADE,
    code            VARCHAR(6)  NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    is_used         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_by      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX uq_plan_sync_codes_unused ON plan_sync_codes (code) WHERE NOT is_used;

-- FN-AUTH-02: one pending OTP per user, stored hashed
CREATE TABLE email_otps (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    code_hash       VARCHAR(100) NOT NULL,
    expires_at      TIMESTAMPTZ  NOT NULL,
    failed_attempts INTEGER      NOT NULL DEFAULT 0 CHECK (failed_attempts >= 0),
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL
);

-- FN-MSG-01: one room per pair of users (user_a_id < user_b_id)
CREATE TABLE chat_rooms (
    id              UUID PRIMARY KEY,
    user_a_id       UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    user_b_id       UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    last_message_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    UNIQUE (user_a_id, user_b_id),
    CHECK (user_a_id < user_b_id)
);
CREATE INDEX idx_chat_rooms_user_b ON chat_rooms (user_b_id);

CREATE TABLE chat_messages (
    id             UUID PRIMARY KEY,
    room_id        UUID          NOT NULL REFERENCES chat_rooms (id) ON DELETE CASCADE,
    sender_id      UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    content        VARCHAR(2000) NOT NULL,
    attachment_url VARCHAR(500),
    created_at     TIMESTAMPTZ   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_chat_messages_room ON chat_messages (room_id, created_at);

-- FN-NOTI-01 / feature spec §6.2: recipient_user_id NULL = studio team (admin feed); set = one mobile user's inbox
ALTER TABLE notifications ADD COLUMN recipient_user_id UUID REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE notifications ADD COLUMN kind VARCHAR(20);
ALTER TABLE notifications ADD COLUMN link VARCHAR(500);
CREATE INDEX idx_notifications_recipient ON notifications (recipient_user_id, created_at);
