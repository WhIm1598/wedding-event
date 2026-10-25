package com.weddingevent.client.auth;

/** Verifies a Google ID token from the mobile Google Sign-In SDK (POST /auth/google). */
public interface GoogleIdTokenVerifier {

    /** {@code subject} = Google's stable user id ({@code sub}); the e-mail of a Google account can change */
    record GoogleProfile(String subject, String email, String name, String pictureUrl, boolean emailVerified) {}

    /** @throws com.weddingevent.common.exception.AppException 401 INVALID_GOOGLE_TOKEN when the token isn't valid */
    GoogleProfile verify(String idToken);
}
