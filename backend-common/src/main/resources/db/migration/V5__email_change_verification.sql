-- FN-AUTH-04 (v1.1.3): a new e-mail is kept apart until its OTP is confirmed, so an account can't claim an
-- address it doesn't own (and later be signed into by that address's real owner through Google).
ALTER TABLE users ADD COLUMN pending_email VARCHAR(255);

-- Each code proves the one address it was sent to (the account's e-mail or its pending one).
ALTER TABLE email_otps ADD COLUMN address VARCHAR(255);
UPDATE email_otps o SET address = u.email FROM users u WHERE u.id = o.user_id;
ALTER TABLE email_otps ALTER COLUMN address SET NOT NULL;
CREATE INDEX idx_email_otps_address ON email_otps (lower(address));
