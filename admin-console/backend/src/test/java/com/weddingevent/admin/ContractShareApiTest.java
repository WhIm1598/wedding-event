package com.weddingevent.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.weddingevent.common.domain.Contract;
import com.weddingevent.common.domain.StudioSettings;
import com.weddingevent.common.repository.ContractRepository;
import com.weddingevent.common.repository.StudioSettingsRepository;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** FN-ADM-CONTR-01 endpoint 3 (ZNS via the studio's Zalo OA), FN-ADM-SET-02, FN-PUB-CONTR-01 public contract page */
class ContractShareApiTest extends IntegrationTest {

    @Autowired
    StudioSettingsRepository settings;

    @Autowired
    ContractRepository contracts;

    /** A connected OA without a cached access token */
    @BeforeEach
    void connectedOa() {
        ZALO.reset();
        StudioSettings s = settings.findById(StudioSettings.SINGLETON_ID).orElseThrow();
        s.setZaloOaAppId("app-1");
        s.setZaloOaAppSecret("secret-1");
        s.setZaloOaRefreshToken("refresh-1");
        s.setZnsContractTemplateId("tpl-1");
        s.setZaloOaAccessToken(null);
        s.setZaloOaAccessTokenExpiresAt(null);
        settings.save(s);
    }

    private ResultActions share(String token, String contractId, String body) throws Exception {
        var req = authed(post("/api/v1/admin/contracts/" + contractId + "/share-zalo"), token);
        return mvc.perform(body == null ? req : req.content(json(body)));
    }

    private String sharedUrl(String contractId) throws Exception {
        String body = share(adminToken(), contractId, null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return read(body, "$.data.publicUrl");
    }

    private static String tokenOf(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    private ResultActions publicGet(String token) throws Exception {
        return mvc.perform(get("/api/v1/public/contracts/" + token));
    }

    private static String form(ZaloStub.Request r, String key) {
        for (String pair : r.body().split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv[0].equals(key)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    // --- FN-ADM-SET-02 ---

    @Test
    void zaloOaSettingsAreAdminOnlyAndNeverRevealSecrets() throws Exception {
        mvc.perform(authed(get("/api/v1/admin/settings/zalo-oa"), staffToken())).andExpect(status().isForbidden());

        String body = body(put("/api/v1/admin/settings/zalo-oa")
                .content(json("{'appId':'app-2','appSecret':'','refreshToken':'refresh-2','templateId':'tpl-2'}")), adminToken());
        assertThat(read(body, "$.data.configured")).isEqualTo("true");
        assertThat(body).doesNotContain("secret-1").doesNotContain("refresh-2");

        StudioSettings s = settings.findById(StudioSettings.SINGLETON_ID).orElseThrow();
        assertThat(s.getZaloOaAppSecret()).isEqualTo("secret-1"); // blank keeps the stored secret
        assertThat(s.getZaloOaRefreshToken()).isEqualTo("refresh-2");

        mvc.perform(authed(get("/api/v1/admin/settings/zalo-oa"), adminToken()))
                .andExpect(jsonPath("$.data.appId").value("app-2"))
                .andExpect(jsonPath("$.data.hasAppSecret").value(true))
                .andExpect(jsonPath("$.data.appSecret").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
    }

    // --- share-zalo ---

    @Test
    void sharingSendsTheZnsWithAFreshLinkAndRotatesTheRefreshToken() throws Exception {
        String phone = uniquePhone();
        String id = createContract(adminToken(), phone, 20_000_000, 0);

        String body = share(staffToken(), id, null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(read(body, "$.data.sentTo")).isEqualTo(phone.substring(0, 4) + "***" + phone.substring(7));
        String url = read(body, "$.data.publicUrl");
        assertThat(url).startsWith("https://studio.test/c/");

        ZaloStub.Request tokenCall = ZALO.requestsTo("/oauth").getFirst();
        assertThat(tokenCall.header("secret_key")).isEqualTo("secret-1");
        assertThat(form(tokenCall, "refresh_token")).isEqualTo("refresh-1");
        assertThat(form(tokenCall, "app_id")).isEqualTo("app-1");
        assertThat(form(tokenCall, "grant_type")).isEqualTo("refresh_token");

        ZaloStub.Request zns = ZALO.requestsTo("/zns").getFirst();
        assertThat(zns.header("access_token")).isEqualTo("stub-access");
        assertThat(read(zns.body(), "$.phone")).isEqualTo("84" + phone.substring(1));
        assertThat(read(zns.body(), "$.template_id")).isEqualTo("tpl-1");
        assertThat(read(zns.body(), "$.template_data.contract_url")).isEqualTo(url);
        assertThat(read(zns.body(), "$.template_data.total_amount")).isEqualTo("20000000");
        assertThat(read(zns.body(), "$.tracking_id")).matches("[A-Za-z0-9]{1,48}");

        StudioSettings s = settings.findById(StudioSettings.SINGLETON_ID).orElseThrow();
        assertThat(s.getZaloOaRefreshToken()).isEqualTo("stub-refresh-next");
        assertThat(s.getZaloOaAccessTokenExpiresAt()).isAfter(Instant.now().plusSeconds(24 * 3600));

        // The cached access token is reused; sharing again replaces the link
        String second = sharedUrl(id);
        assertThat(ZALO.requestsTo("/oauth")).hasSize(1);
        publicGet(tokenOf(url)).andExpect(status().isGone()).andExpect(jsonPath("$.code").value("SHARE_LINK_EXPIRED"));
        publicGet(tokenOf(second)).andExpect(status().isOk());
    }

    @Test
    void aCustomerWithoutZaloNeedsAnotherNumber() throws Exception {
        String pkgId = read(body(get("/api/v1/admin/packages"), adminToken()), "$.data[0].id");
        String created = body(post("/api/v1/admin/contracts").content(json("""
                {'customerName':'Khách Không Zalo','phone':'%s','servicePackageId':'%s','totalAmount':1000000,'depositAmount':0,'hasZalo':false}
                """.formatted(uniquePhone(), pkgId))), adminToken());
        String id = read(created, "$.data.id");

        share(adminToken(), id, null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ZALO_NOT_AVAILABLE"));
        share(adminToken(), id, "{'phoneNumber':'12345'}").andExpect(status().isBadRequest());
        share(adminToken(), id, "{'phoneNumber':'0912345678'}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sentTo").value("0912***678"));
        assertThat(read(ZALO.requestsTo("/zns").getFirst().body(), "$.phone")).isEqualTo("84912345678");
    }

    @Test
    void anUnconnectedOaIsReported() throws Exception {
        StudioSettings s = settings.findById(StudioSettings.SINGLETON_ID).orElseThrow();
        s.setZaloOaRefreshToken(null);
        settings.save(s);
        share(adminToken(), createContract(adminToken(), uniquePhone(), 1_000_000, 0), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ZALO_OA_NOT_CONFIGURED"));
        assertThat(ZALO.requests).isEmpty();
    }

    @Test
    void aRefusedMessageSavesNoLinkButKeepsTheRotatedRefreshToken() throws Exception {
        ZALO.onZns(r -> "{\"error\":-108,\"message\":\"Phone number is invalid\"}");
        String id = createContract(adminToken(), uniquePhone(), 1_000_000, 0);

        share(adminToken(), id, null).andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("ZALO_SEND_FAILED"));

        assertThat(contracts.findById(UUID.fromString(id)).orElseThrow().getPublicTokenHash()).isNull();
        // The single-use refresh token was spent: the new one must have been kept despite the rollback
        assertThat(settings.findById(StudioSettings.SINGLETON_ID).orElseThrow().getZaloOaRefreshToken()).isEqualTo("stub-refresh-next");
    }

    @Test
    void aRefusedResendKeepsThePreviousLinkWorking() throws Exception {
        String id = createContract(adminToken(), uniquePhone(), 1_000_000, 0);
        String token = tokenOf(sharedUrl(id));

        ZALO.onZns(r -> "{\"error\":-108,\"message\":\"Phone number is invalid\"}");
        share(adminToken(), id, null).andExpect(status().isBadGateway());
        publicGet(token).andExpect(status().isOk());
    }

    @Test
    void aLostAnswerKeepsTheNewLinkBecauseTheMessageMayHaveArrived() throws Exception {
        String id = createContract(adminToken(), uniquePhone(), 1_000_000, 0);
        String old = tokenOf(sharedUrl(id));

        ZALO.onZns(r -> null);
        share(adminToken(), id, null).andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("ZALO_SEND_FAILED"));

        String sentUrl = read(ZALO.requestsTo("/zns").getLast().body(), "$.template_data.contract_url");
        publicGet(tokenOf(sentUrl)).andExpect(status().isOk());
        publicGet(old).andExpect(status().isGone());
    }

    @Test
    void aRejectedAccessTokenIsRefreshedOnTheNextShare() throws Exception {
        ZALO.onZns(r -> "{\"error\":-124,\"message\":\"Access token is invalid\"}");
        String id = createContract(adminToken(), uniquePhone(), 1_000_000, 0);
        share(adminToken(), id, null).andExpect(status().isBadGateway());

        ZALO.onZns(r -> ZaloStub.ZNS_OK);
        sharedUrl(id);
        assertThat(ZALO.requestsTo("/oauth")).hasSize(2);
    }

    @Test
    void aFailedTokenRefreshAsksTheAdminToReconnect() throws Exception {
        ZALO.onToken(r -> "{\"error\":-14014,\"message\":\"Invalid refresh token\"}");
        share(adminToken(), createContract(adminToken(), uniquePhone(), 1_000_000, 0), null)
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("ZALO_OA_AUTH_FAILED"));
        assertThat(ZALO.requestsTo("/zns")).isEmpty();
    }

    // --- FN-PUB-CONTR-01 ---

    @Test
    void thePublicPageShowsTheContractWithoutPrivateData() throws Exception {
        String phone = uniquePhone();
        String id = createContract(adminToken(), phone, 20_000_000, 6_000_000);
        String token = tokenOf(sharedUrl(id));

        String body = publicGet(token).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(read(body, "$.data.phone")).isEqualTo(phone.substring(0, 4) + "***" + phone.substring(7));
        assertThat(body).doesNotContain(phone).doesNotContain("\"notes\"").doesNotContain("\"id\"");
        assertThat(read(body, "$.data.paidAmount")).isEqualTo("6000000");
        assertThat(read(body, "$.data.studio.bankInfo")).isNotBlank();
        List<Integer> amounts = JsonPath.read(body, "$.data.installments[*].amount");
        assertThat(amounts).containsExactly(6_000_000, 10_000_000, 4_000_000);
        assertThat(read(body, "$.data.acceptedAt")).isEqualTo("null");

        mvc.perform(get("/api/v1/public/contracts/" + token + "/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void theCustomerAcceptsOnce() throws Exception {
        String id = createContract(adminToken(), uniquePhone(), 1_000_000, 0);
        String token = tokenOf(sharedUrl(id));

        String first = mvc.perform(post("/api/v1/public/contracts/" + token + "/accept")
                        .header("User-Agent", "Zalo iOS").header("X-Forwarded-For", "203.0.113.7"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String acceptedAt = read(first, "$.data.acceptedAt");
        assertThat(acceptedAt).isNotEqualTo("null");

        String again = mvc.perform(post("/api/v1/public/contracts/" + token + "/accept"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(read(again, "$.data.acceptedAt")).isEqualTo(acceptedAt);

        Contract c = contracts.findById(UUID.fromString(id)).orElseThrow();
        assertThat(c.getAcceptedUserAgent()).isEqualTo("Zalo iOS");
        assertThat(c.getAcceptedIp()).isNotBlank();
    }

    @Test
    void unknownExpiredAndRevokedLinksAreGone() throws Exception {
        publicGet("khong-ton-tai").andExpect(status().isGone()).andExpect(jsonPath("$.code").value("SHARE_LINK_EXPIRED"));

        String id = createContract(adminToken(), uniquePhone(), 1_000_000, 0);
        String token = tokenOf(sharedUrl(id));
        Contract c = contracts.findById(UUID.fromString(id)).orElseThrow();
        c.setShareExpiresAt(Instant.now().minusSeconds(1));
        contracts.save(c);
        publicGet(token).andExpect(status().isGone());

        String fresh = tokenOf(sharedUrl(id));
        mvc.perform(authed(post("/api/v1/admin/contracts/" + id + "/revoke-link"), staffToken())).andExpect(status().isForbidden());
        mvc.perform(authed(post("/api/v1/admin/contracts/" + id + "/revoke-link"), adminToken())).andExpect(status().isOk());
        publicGet(fresh).andExpect(status().isGone());
        mvc.perform(post("/api/v1/public/contracts/" + fresh + "/accept")).andExpect(status().isGone());
    }
}
