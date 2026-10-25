package com.weddingevent.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.weddingevent.common.domain.EmailOtp;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.repository.EmailOtpRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** FN-AUTH-01 (login), FN-AUTH-02 (OTP), Google login (feature spec §7.1), GET /auth/me, token rules */
class AuthApiTest extends ClientIntegrationTest {

    @Autowired
    EmailOtpRepository otps;

    private org.springframework.test.web.servlet.ResultActions loginCall(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json("{'email':'" + email + "','password':'" + password + "'}")));
    }

    private org.springframework.test.web.servlet.ResultActions verify(String email, String code) throws Exception {
        return mvc.perform(post("/api/v1/auth/verify-otp").contentType(MediaType.APPLICATION_JSON)
                .content(json("{'email':'" + email + "','otpCode':'" + code + "'}")));
    }

    private static String wrong(String code) {
        return code.equals("0000") ? "1111" : "0000";
    }

    @Test
    void loginReturnsTokensAndTheUserObject() throws Exception {
        User u = createUser(null, true);
        loginCall(u.getEmail().toUpperCase(), PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.user.id").value(u.getId().toString()))
                .andExpect(jsonPath("$.data.user.email").value(u.getEmail()))
                .andExpect(jsonPath("$.data.user.fullName").value(u.getFullName()))
                .andExpect(jsonPath("$.data.user.role").doesNotExist())
                .andExpect(jsonPath("$.data.user.hasPlan").value(false))
                .andExpect(jsonPath("$.data.user.isVerified").value(true));
    }

    @Test
    void wrongPasswordAndUnknownEmailLookIdentical() throws Exception {
        User u = createUser(Role.ROLE_COUPLE, true);
        for (String email : new String[] {u.getEmail(), "nobody-" + UUID.randomUUID() + "@example.com"}) {
            loginCall(email, "wrong-password")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
    }

    @Test
    void loginValidatesEmailFormatAndPasswordLength() throws Exception {
        loginCall("not-an-email", "12345")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void lockedAccountGets403() throws Exception {
        User u = createUser(Role.ROLE_COUPLE, true);
        u.setActive(false);
        users.save(u);
        loginCall(u.getEmail(), PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void studioAccountsCannotUseTheMobileApi() throws Exception {
        for (Role role : new Role[] {Role.ROLE_ADMIN, Role.ROLE_STAFF}) {
            User u = createUser(role, true);
            loginCall(u.getEmail(), PASSWORD)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
    }

    @Test
    void unverifiedLoginSendsAnOtpThatVerifiesTheAccount() throws Exception {
        User u = createUser(null, false);
        String token = read(body(loginCall(u.getEmail(), PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.isVerified").value(false))), "$.data.accessToken");

        String code = otpSender.lastCodeFor(u.getEmail());
        assertThat(code).matches("\\d{4}");
        assertThat(otps.findByUserId(u.getId()).orElseThrow().getCodeHash()).isNotEqualTo(code); // stored hashed

        verify(u.getEmail(), wrong(code))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OTP"));
        verify(u.getEmail(), code).andExpect(status().isOk());

        mvc.perform(authed(get("/api/v1/auth/me"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isVerified").value(true));
        assertThat(otps.findByUserId(u.getId())).isEmpty();
        verify(u.getEmail(), code)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OTP")); // single use
    }

    @Test
    void fiveWrongAttemptsDisableTheCodeUntilANewOneIsSent() throws Exception {
        User u = createUser(null, false);
        loginCall(u.getEmail(), PASSWORD).andExpect(status().isOk());
        String code = otpSender.lastCodeFor(u.getEmail());

        for (int i = 0; i < 5; i++) {
            verify(u.getEmail(), wrong(code)).andExpect(jsonPath("$.code").value("INVALID_OTP"));
        }
        verify(u.getEmail(), code)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OTP_EXPIRED"));

        mvc.perform(post("/api/v1/auth/send-otp").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'email':'" + u.getEmail() + "'}")))
                .andExpect(status().isOk());
        verify(u.getEmail(), otpSender.lastCodeFor(u.getEmail())).andExpect(status().isOk());
    }

    @Test
    void atMostFiveCodesAnHourSoTheCodeCantBeGuessed() throws Exception {
        // 5 codes × 5 attempts = 25 guesses an hour out of 10 000 codes
        User u = createUser(null, false);
        loginCall(u.getEmail(), PASSWORD).andExpect(status().isOk()); // 1st code
        for (int i = 0; i < 6; i++) {
            mvc.perform(post("/api/v1/auth/send-otp").contentType(MediaType.APPLICATION_JSON)
                            .content(json("{'email':'" + u.getEmail() + "'}")))
                    .andExpect(status().isOk()); // never reveals the limit
        }
        assertThat(otpSender.sendCount(u.getEmail())).isEqualTo(5);

        // A refused resend doesn't hand out 5 fresh guesses
        String code = otpSender.lastCodeFor(u.getEmail());
        for (int i = 0; i < 5; i++) {
            verify(u.getEmail(), wrong(code));
        }
        mvc.perform(post("/api/v1/auth/send-otp").contentType(MediaType.APPLICATION_JSON)
                .content(json("{'email':'" + u.getEmail() + "'}")));
        verify(u.getEmail(), code).andExpect(jsonPath("$.code").value("OTP_EXPIRED"));

        // Next hour: sending works again
        EmailOtp otp = otps.findByUserId(u.getId()).orElseThrow();
        otp.setSendWindowStart(Instant.now().minus(61, ChronoUnit.MINUTES));
        otps.save(otp);
        mvc.perform(post("/api/v1/auth/send-otp").contentType(MediaType.APPLICATION_JSON)
                .content(json("{'email':'" + u.getEmail() + "'}")));
        verify(u.getEmail(), otpSender.lastCodeFor(u.getEmail())).andExpect(status().isOk());
    }

    @Test
    void codeExpiresAfterFiveMinutes() throws Exception {
        User u = createUser(null, false);
        loginCall(u.getEmail(), PASSWORD).andExpect(status().isOk());
        EmailOtp otp = otps.findByUserId(u.getId()).orElseThrow();
        assertThat(otp.getExpiresAt()).isBetween(Instant.now().plus(4, ChronoUnit.MINUTES), Instant.now().plus(6, ChronoUnit.MINUTES));
        otp.setExpiresAt(Instant.now().minusSeconds(1));
        otps.save(otp);

        verify(u.getEmail(), otpSender.lastCodeFor(u.getEmail()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OTP_EXPIRED"));
    }

    @Test
    void sendOtpNeverRevealsWhetherAnAccountExists() throws Exception {
        User verified = createUser(Role.ROLE_COUPLE, true);
        for (String email : new String[] {"ghost-" + UUID.randomUUID() + "@example.com", verified.getEmail()}) {
            mvc.perform(post("/api/v1/auth/send-otp").contentType(MediaType.APPLICATION_JSON)
                            .content(json("{'email':'" + email + "'}")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
            assertThat(otpSender.lastCodeFor(email)).isNull();
        }
    }

    @Test
    void otpMustBeFourDigits() throws Exception {
        verify("someone@example.com", "12a4")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("otpCode"));
    }

    @Test
    void googleLoginCreatesAnAccountWithoutRoleThenReusesIt() throws Exception {
        String email = "g-" + UUID.randomUUID() + "@gmail.com";
        String token = "google|" + email + "|Ngọc Google|true";
        String first = body(mvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'idToken':'" + token + "'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andExpect(jsonPath("$.data.user.fullName").value("Ngọc Google"))
                .andExpect(jsonPath("$.data.user.avatarUrl").value("https://example.com/a.png"))
                .andExpect(jsonPath("$.data.user.role").doesNotExist())
                .andExpect(jsonPath("$.data.user.isVerified").value(true)));

        mvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'idToken':'" + token + "'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.id").value(read(first, "$.data.user.id")));
    }

    @Test
    void googleLoginLinksAnExistingAccountAndRespectsLocksAndStudioRoles() throws Exception {
        User couple = createUser(Role.ROLE_COUPLE, true);
        mvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'idToken':'google|" + couple.getEmail() + "|X|true'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.id").value(couple.getId().toString()))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_COUPLE"));

        couple.setActive(false);
        users.save(couple);
        mvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'idToken':'google|" + couple.getEmail() + "|X|true'}")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));

        User staff = createUser(Role.ROLE_STAFF, true);
        mvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'idToken':'google|" + staff.getEmail() + "|X|true'}")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    private org.springframework.test.web.servlet.ResultActions google(String email, String sub) throws Exception {
        return mvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON)
                .content(json("{'idToken':'google|" + email + "|Người Dùng|true|" + sub + "'}")));
    }

    @Test
    void googleAccountIsRecognisedByItsGoogleIdEvenAfterItsEmailChanges() throws Exception {
        String sub = "sub-" + UUID.randomUUID();
        String first = body(google("old-" + UUID.randomUUID() + "@gmail.com", sub).andExpect(status().isOk()));
        String id = read(first, "$.data.user.id");
        assertThat(users.findById(UUID.fromString(id)).orElseThrow().getGoogleSub()).isEqualTo(sub);

        // Same Google account, new address on Google's side: same WedPlanner account, not a new empty one
        google("new-" + UUID.randomUUID() + "@gmail.com", sub)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.id").value(id));
    }

    @Test
    void anExistingAccountIsLinkedToItsGoogleIdOnFirstGoogleLogin() throws Exception {
        User couple = createUser(Role.ROLE_COUPLE, true);
        String sub = "sub-" + UUID.randomUUID();
        google(couple.getEmail(), sub).andExpect(jsonPath("$.data.user.id").value(couple.getId().toString()));
        assertThat(users.findById(couple.getId()).orElseThrow().getGoogleSub()).isEqualTo(sub);
    }

    @Test
    void aRecycledGoogleAddressCannotTakeOverTheAccount() throws Exception {
        // The address now belongs to a different Google account (e.g. a recycled Gmail address)
        String email = "recycled-" + UUID.randomUUID() + "@gmail.com";
        google(email, "sub-original-" + UUID.randomUUID()).andExpect(status().isOk());
        google(email, "sub-newcomer-" + UUID.randomUUID())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_EXISTS"));
    }

    @Test
    void googleLoginRejectsInvalidTokens() throws Exception {
        mvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'idToken':'forged'}")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_GOOGLE_TOKEN"));
        mvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void googleLoginNeedsAnAddressGoogleHasVerified() throws Exception {
        // Otherwise anyone could create an account holding someone else's address and wait for the owner
        String email = "g-" + UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'idToken':'google|" + email + "|Khách|false'}")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_GOOGLE_TOKEN"));
        assertThat(users.findByEmailIgnoreCase(email)).isEmpty();
    }

    @Test
    void googleLoginDoesNotTakeOverAnAccountWhoseAddressWasNeverVerified() throws Exception {
        User unverified = createUser(Role.ROLE_COUPLE, false);
        mvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'idToken':'google|" + unverified.getEmail() + "|X|true'}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_EXISTS"));
        assertThat(users.findById(unverified.getId()).orElseThrow().isVerified()).isFalse();
    }

    @Test
    void meReturnsTheCurrentUser() throws Exception {
        User u = createUser(Role.ROLE_VENDOR, true);
        mvc.perform(authed(get("/api/v1/auth/me"), token(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(u.getId().toString()))
                .andExpect(jsonPath("$.data.role").value("ROLE_VENDOR"));
    }

    @Test
    void apiRequiresAValidAccessTokenOfThisService() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        User u = createUser(Role.ROLE_COUPLE, true);
        String refresh = read(body(loginCall(u.getEmail(), PASSWORD)), "$.data.refreshToken");
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + refresh))
                .andExpect(status().isUnauthorized());

        // Same signing secret, but issued by the admin backend
        var key = new SecretKeySpec("test-only-secret-0123456789abcdefghijklmnop".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Instant now = Instant.now();
        String adminToken = new NimbusJwtEncoder(new ImmutableSecret<>(key)).encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer("wedding-event-admin").subject(u.getId().toString()).issuedAt(now)
                        .expiresAt(now.plusSeconds(600)).claim("typ", "access").build())).getTokenValue();
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokensStopWorkingWhenTheAccountIsLocked() throws Exception {
        User u = createUser(Role.ROLE_COUPLE, true);
        String token = token(u);
        mvc.perform(authed(get("/api/v1/auth/me"), token)).andExpect(status().isOk());
        u.setActive(false);
        users.save(u);
        mvc.perform(authed(get("/api/v1/auth/me"), token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void unknownRoutesAreDenied() throws Exception {
        mvc.perform(authed(get("/api/v1/admin/packages"), coupleToken())).andExpect(status().isForbidden());
    }
}
