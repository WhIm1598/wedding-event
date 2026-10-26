package com.weddingevent.admin.zalo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.weddingevent.admin.config.AppProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Zalo Official Account HTTP API: OAuth v4 token refresh and ZNS template messages. Zalo answers HTTP 200 with
 * {@code error != 0} on failure, so both calls check the body. Never logs tokens or phone numbers.
 */
@Component
public class ZaloOaClient {

    private static final Logger log = LoggerFactory.getLogger(ZaloOaClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    public record Tokens(String accessToken, String refreshToken, Duration expiresIn) {}

    /** {@code error} is Zalo's code (0 = success); -1 when Zalo couldn't be reached or answered garbage */
    public static class ZaloException extends RuntimeException {
        private final int error;

        ZaloException(int error, String message) {
            super(message);
            this.error = error;
        }

        public int error() {
            return error;
        }
    }

    record TokenResponse(@JsonProperty("access_token") String accessToken, @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") String expiresIn, Integer error, String message) {}

    record ZnsRequest(String phone, @JsonProperty("template_id") String templateId,
            @JsonProperty("template_data") Map<String, String> templateData, @JsonProperty("tracking_id") String trackingId) {}

    record ZnsResponse(Integer error, String message, Map<String, Object> data) {}

    private final RestClient rest;
    private final AppProperties.Zalo urls;

    public ZaloOaClient(AppProperties props) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
        factory.setReadTimeout(TIMEOUT);
        this.rest = RestClient.builder().requestFactory(factory).build();
        this.urls = props.zalo();
    }

    /** New access token (about 25 h) and a new single-use refresh token (3 months) */
    public Tokens refresh(String appId, String appSecret, String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("refresh_token", refreshToken);
        form.add("app_id", appId);
        form.add("grant_type", "refresh_token");
        TokenResponse res;
        try {
            res = rest.post().uri(urls.oauthUrl())
                    .header("secret_key", appSecret)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
        } catch (RestClientException e) {
            log.warn("Zalo OA token refresh failed: {}", e.getMessage());
            throw new ZaloException(-1, e.getMessage());
        }
        if (res == null || res.accessToken() == null || res.refreshToken() == null) {
            int error = res == null || res.error() == null ? -1 : res.error();
            log.warn("Zalo OA token refresh refused: error {}", error);
            throw new ZaloException(error, res == null ? "empty response" : res.message());
        }
        return new Tokens(res.accessToken(), res.refreshToken(), Duration.ofSeconds(parseSeconds(res.expiresIn())));
    }

    /** Sends an approved ZNS template to {@code phone} (84xxxxxxxxx); returns Zalo's msg_id */
    public String sendTemplate(String accessToken, String phone, String templateId, Map<String, String> data, String trackingId) {
        ZnsResponse res;
        try {
            res = rest.post().uri(urls.znsUrl())
                    .header("access_token", accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ZnsRequest(phone, templateId, data, trackingId))
                    .retrieve()
                    .body(ZnsResponse.class);
        } catch (RestClientException e) {
            log.warn("ZNS send failed: {}", e.getMessage());
            throw new ZaloException(-1, e.getMessage());
        }
        if (res == null || res.error() == null || res.error() != 0) {
            int error = res == null || res.error() == null ? -1 : res.error();
            log.warn("ZNS send refused: error {} {}", error, res == null ? "" : res.message());
            throw new ZaloException(error, res == null ? "empty response" : res.message());
        }
        return res.data() == null ? null : String.valueOf(res.data().get("msg_id"));
    }

    /** Zalo sends expires_in as a string of seconds; assume the documented 25 h when it is missing */
    private static long parseSeconds(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException | NullPointerException e) {
            return Duration.ofHours(25).toSeconds();
        }
    }
}
