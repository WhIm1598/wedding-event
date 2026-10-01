package com.weddingevent.admin.contract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sends the contract link via Zalo ZNS / OA (FN-ADM-CONTR-01 endpoint 3).
 * TODO: replace with a real ZNS client (OA access token, approved template id) once credentials exist.
 */
public interface ZaloClient {

    void sendContractLink(String phoneNumber, String customerName, String contractNumber, String link);

    @Component
    class LoggingZaloClient implements ZaloClient {

        private static final Logger log = LoggerFactory.getLogger(LoggingZaloClient.class);

        @Override
        public void sendContractLink(String phoneNumber, String customerName, String contractNumber, String link) {
            String masked = phoneNumber.length() > 4 ? "******" + phoneNumber.substring(phoneNumber.length() - 4) : "****";
            log.info("[ZALO-STUB] Contract {} for {} -> {} ({})", contractNumber, customerName, masked, link);
        }
    }
}
