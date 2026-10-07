package com.weddingevent.client.auth;

/** Delivers the 4-digit e-mail verification code (FN-AUTH-02). */
public interface OtpSender {

    void send(String email, String code);
}
