package com.weddingevent.client.notification;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.weddingevent.common.domain.DeviceToken;
import com.weddingevent.common.repository.DeviceTokenRepository;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FN-NOTI-02: Firebase Cloud Messaging HTTP v1. Authenticates with the service account JSON (OAuth2 JWT bearer grant,
 * signed with Nimbus — no Firebase SDK needed) and sends one message per registered device, off the request thread.
 * Tokens FCM reports as dead are deleted.
 */
public class FcmPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(FcmPushSender.class);
    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    private final String sendUrl;
    private final String clientEmail;
    private final String tokenUri;
    private final PrivateKey privateKey;
    private final DeviceTokenRepository devices;
    private final Clock clock;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Executor executor;

    private String accessToken;
    private Instant accessTokenExpiry = Instant.EPOCH;

    public FcmPushSender(String projectId, Path credentialsFile, String baseUrl, DeviceTokenRepository devices, Clock clock,
            Executor executor) throws IOException, ParseException, GeneralSecurityException {
        Map<String, Object> credentials = JSONObjectUtils.parse(Files.readString(credentialsFile));
        this.clientEmail = (String) credentials.get("client_email");
        this.tokenUri = (String) credentials.get("token_uri");
        this.privateKey = parsePkcs8((String) credentials.get("private_key"));
        this.sendUrl = baseUrl + "/v1/projects/" + projectId + "/messages:send";
        this.devices = devices;
        this.clock = clock;
        this.executor = executor;
    }

    public FcmPushSender(String projectId, Path credentialsFile, String baseUrl, DeviceTokenRepository devices, Clock clock)
            throws IOException, ParseException, GeneralSecurityException {
        this(projectId, credentialsFile, baseUrl, devices, clock, Executors.newVirtualThreadPerTaskExecutor());
    }

    @Override
    public void send(UUID userId, PushMessage message) {
        executor.execute(() -> deliver(userId, message));
    }

    private void deliver(UUID userId, PushMessage message) {
        for (DeviceToken device : devices.findAllByUserIdOrderByUpdatedAtAsc(userId)) {
            try {
                sendTo(device.getToken(), message);
            } catch (Exception e) {
                log.warn("FCM push to a device of user {} failed: {}", userId, e.toString());
            }
        }
    }

    private void sendTo(String token, PushMessage m) throws IOException, InterruptedException, JOSEException, ParseException {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("token", token);
        if (m.title() != null) {
            message.put("notification", Map.of("title", m.title(), "body", m.body() == null ? "" : m.body()));
        } else {
            // Data-only: lets iOS wake the app in the background
            message.put("apns", Map.of("payload", Map.of("aps", Map.of("content-available", 1))));
        }
        if (m.data() != null && !m.data().isEmpty()) {
            message.put("data", m.data());
        }
        message.put("android", Map.of("priority", "HIGH"));

        HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create(sendUrl))
                        .timeout(Duration.ofSeconds(10))
                        .header("Authorization", "Bearer " + accessToken())
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(JSONObjectUtils.toJSONString(Map.of("message", message))))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 404 || res.body().contains("UNREGISTERED")
                || (res.statusCode() == 400 && res.body().contains("INVALID_ARGUMENT"))) {
            devices.deleteByTokenValue(token);
        } else if (res.statusCode() >= 300) {
            log.warn("FCM answered {}: {}", res.statusCode(), res.body());
        }
    }

    /** OAuth2 access token for FCM, cached until a minute before it expires */
    private synchronized String accessToken() throws IOException, InterruptedException, JOSEException, ParseException {
        Instant now = clock.instant();
        if (accessToken != null && now.isBefore(accessTokenExpiry.minusSeconds(60))) {
            return accessToken;
        }
        SignedJWT assertion = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), new JWTClaimsSet.Builder()
                .issuer(clientEmail)
                .audience(tokenUri)
                .claim("scope", SCOPE)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)))
                .build());
        assertion.sign(new RSASSASigner(privateKey));
        String form = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", StandardCharsets.UTF_8)
                + "&assertion=" + assertion.serialize();
        HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create(tokenUri))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new IOException("FCM OAuth token request answered " + res.statusCode());
        }
        Map<String, Object> body = JSONObjectUtils.parse(res.body());
        accessToken = (String) body.get("access_token");
        accessTokenExpiry = now.plusSeconds(((Number) body.get("expires_in")).longValue());
        return accessToken;
    }

    private static PrivateKey parsePkcs8(String pem) throws GeneralSecurityException {
        String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
    }
}
