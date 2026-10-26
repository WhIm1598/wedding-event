-- FN-ADM-AUTH-01 step 3 (#38): 5 wrong admin-console passwords in a row lock the login for 15 minutes.
ALTER TABLE users
    ADD COLUMN failed_login_count INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until       TIMESTAMPTZ;
