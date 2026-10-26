-- FN-ADM-USER-01 / FN-ADM-AUTH-01 step 6: temporary passwords must be changed at first login, the account list shows
-- the last login, and a staff profile has at most one login account.
ALTER TABLE users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN last_login_at        TIMESTAMPTZ;

CREATE UNIQUE INDEX uq_users_staff_member ON users (staff_member_id) WHERE staff_member_id IS NOT NULL;
