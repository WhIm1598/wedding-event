-- FN-NOTI-02 (v1.1.5): FCM registration tokens of the mobile users' devices.
CREATE TABLE device_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token      VARCHAR(4096) NOT NULL UNIQUE,
    platform   VARCHAR(10)   NOT NULL CHECK (platform IN ('ANDROID', 'IOS')),
    created_at TIMESTAMPTZ   NOT NULL,
    updated_at TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_device_tokens_user ON device_tokens (user_id, updated_at);
