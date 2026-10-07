package com.weddingevent.client.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder until an e-mail provider is chosen (TODO: SMTP / transactional e-mail). Logs a masked address; prints
 * the code itself only when {@code app.otp.log-code=true} (dev profile), so it never reaches production logs.
 */
public class LoggingOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

    private final boolean logCode;

    public LoggingOtpSender(boolean logCode) {
        this.logCode = logCode;
    }

    @Override
    public void send(String email, String code) {
        if (logCode) {
            log.info("OTP for {}: {} (dev only — no e-mail sender configured)", mask(email), code);
        } else {
            log.info("OTP issued for {} (no e-mail sender configured)", mask(email));
        }
    }

    static String mask(String email) {
        int at = email.indexOf('@');
        return at < 1 ? "***" : email.charAt(0) + "***" + email.substring(at);
    }
}
