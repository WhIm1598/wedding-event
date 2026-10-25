-- FN-AUTH-02 (v1.1.3): at most 5 codes per account per hour. A new code resets the wrong-attempt counter, so
-- without this limit "resend + 5 guesses" would eventually find a 4-digit code.
ALTER TABLE email_otps ADD COLUMN send_count INTEGER NOT NULL DEFAULT 1 CHECK (send_count >= 0);
ALTER TABLE email_otps ADD COLUMN send_window_start TIMESTAMPTZ NOT NULL DEFAULT now();
