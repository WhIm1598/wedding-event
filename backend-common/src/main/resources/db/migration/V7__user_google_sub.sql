-- Google login recognises an account by Google's stable user id (the ID token's `sub`), not by e-mail: a Google
-- account's address can change, and an address can be recycled to a different Google account.
ALTER TABLE users ADD COLUMN google_sub VARCHAR(255);
ALTER TABLE users ADD CONSTRAINT uq_users_google_sub UNIQUE (google_sub);
