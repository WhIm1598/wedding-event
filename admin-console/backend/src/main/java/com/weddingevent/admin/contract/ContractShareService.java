package com.weddingevent.admin.contract;

import com.weddingevent.admin.config.AppProperties;
import com.weddingevent.admin.contract.ContractDocumentRenderer.Installment;
import com.weddingevent.admin.contract.ContractService.Document;
import com.weddingevent.admin.notification.NotificationService;
import com.weddingevent.admin.security.CurrentStudio;
import com.weddingevent.admin.staff.StaffService;
import com.weddingevent.admin.zalo.ZaloOaClient.ZaloException;
import com.weddingevent.admin.zalo.ZaloOaClient;
import com.weddingevent.admin.zalo.ZaloOaTokens;
import com.weddingevent.common.domain.Booking;
import com.weddingevent.common.domain.Contract;
import com.weddingevent.common.domain.StudioSettings;
import com.weddingevent.common.domain.enums.BookingStatus;
import com.weddingevent.common.domain.enums.ContractStatus;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.BookingRepository;
import com.weddingevent.common.repository.ContractRepository;
import com.weddingevent.common.repository.StudioSettingsRepository;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FN-ADM-CONTR-01 endpoint 3 (ZNS through the studio's own Zalo OA) and FN-PUB-CONTR-01 (the customer's public
 * contract page). Only the SHA-256 of a link token is stored, so sharing again issues a new token and the previous
 * link stops working.
 */
@Service
public class ContractShareService {

    static final Duration LINK_LIFETIME = Duration.ofDays(30);
    /** Zalo OA API: access token invalid or expired */
    private static final int ZALO_INVALID_ACCESS_TOKEN = -124;

    public record ShareZaloRequest(
            @Pattern(regexp = "^$|" + StaffService.VN_PHONE, message = "Số điện thoại không hợp lệ") String phoneNumber) {}

    public record ShareResultDto(String sentTo, String publicUrl, Instant shareExpiresAt) {}

    public record PublicStudioDto(String name, String address, String bankInfo) {}

    public record PublicContractDto(String contractNumber, PublicStudioDto studio, String customerName, String phone,
            String packageName, List<String> features, BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal remainingAmount,
            List<Installment> installments, LocalDate eventDate, ContractStatus status, Instant acceptedAt) {}

    private final ContractRepository contracts;
    private final BookingRepository bookings;
    private final StudioSettingsRepository settings;
    private final ContractDocumentRenderer renderer;
    private final ZaloOaTokens zaloTokens;
    private final ZaloOaClient zalo;
    private final NotificationService notifications;
    private final AppProperties props;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public ContractShareService(ContractRepository contracts, BookingRepository bookings, StudioSettingsRepository settings,
            ContractDocumentRenderer renderer, ZaloOaTokens zaloTokens, ZaloOaClient zalo, NotificationService notifications,
            AppProperties props, Clock clock) {
        this.contracts = contracts;
        this.bookings = bookings;
        this.settings = settings;
        this.renderer = renderer;
        this.zaloTokens = zaloTokens;
        this.zalo = zalo;
        this.notifications = notifications;
        this.props = props;
        this.clock = clock;
    }

    /**
     * Sends the ZNS with a fresh 30-day link. The link is saved before the call: when Zalo's answer is lost (timeout)
     * the message may have arrived, so the link must work. Only a definite refusal restores the previous link.
     */
    @Transactional(noRollbackFor = AppException.class)
    public ShareResultDto shareZalo(UUID contractId, String phoneNumber) {
        Contract c = contracts.findById(contractId).orElseThrow(() -> new ResourceNotFoundException("hợp đồng", contractId));
        boolean phoneGiven = phoneNumber != null && !phoneNumber.isBlank();
        if (!phoneGiven && !c.isHasZalo()) {
            throw AppException.badRequest("ZALO_NOT_AVAILABLE", "Khách chưa có Zalo, vui lòng nhập số Zalo khác");
        }
        String phone = phoneGiven ? phoneNumber : c.getPhone();
        StudioSettings studio = settings.findById(CurrentStudio.require()).filter(StudioSettings::isZaloOaConfigured)
                .orElseThrow(ZaloOaTokens::notConfigured);
        if (props.publicBaseUrl() == null || props.publicBaseUrl().isBlank()) {
            throw new IllegalStateException("app.public-base-url (env PUBLIC_BASE_URL) is not set");
        }

        String token = newToken();
        String url = props.publicBaseUrl().replaceAll("/+$", "") + "/c/" + token;
        Map<String, String> data = Map.of(
                "customer_name", c.getCustomerName(),
                "contract_number", c.getContractNumber(),
                "total_amount", c.getTotalAmount().toPlainString(),
                "contract_url", url);
        String accessToken = zaloTokens.accessToken();
        String previousHash = c.getPublicTokenHash();
        Instant previousExpiry = c.getShareExpiresAt();
        c.setPublicTokenHash(sha256(token));
        c.setShareExpiresAt(clock.instant().plus(LINK_LIFETIME));
        try {
            zalo.sendTemplate(accessToken, "84" + phone.substring(1), studio.getZnsContractTemplateId(), data,
                    c.getContractNumber().replaceAll("[^A-Za-z0-9]", "") + clock.millis());
        } catch (ZaloException e) {
            if (e.error() != ZaloException.UNKNOWN_OUTCOME) {
                c.setPublicTokenHash(previousHash);
                c.setShareExpiresAt(previousExpiry);
            }
            if (e.error() == ZALO_INVALID_ACCESS_TOKEN) {
                zaloTokens.invalidate();
            }
            throw new AppException(HttpStatus.BAD_GATEWAY, "ZALO_SEND_FAILED",
                    "Zalo không gửi được tin (mã " + e.error() + "). Kiểm tra số Zalo của khách hoặc mẫu ZNS rồi thử lại");
        }
        return new ShareResultDto(mask(phone), url, c.getShareExpiresAt());
    }

    /** FN-PUB-CONTR-01: the shared link stops working; sharing again issues a new one */
    @Transactional
    public void revokeLink(UUID contractId) {
        Contract c = contracts.findById(contractId).orElseThrow(() -> new ResourceNotFoundException("hợp đồng", contractId));
        c.setPublicTokenHash(null);
        c.setShareExpiresAt(null);
    }

    /** Call as {@link com.weddingevent.common.tenant.StudioTenant#ROOT}: the anonymous caller has no studio */
    @Transactional(readOnly = true)
    public UUID studioOfToken(String token) {
        return byToken(token).getStudioId();
    }

    @Transactional(readOnly = true)
    public PublicContractDto publicView(String token) {
        Contract c = byToken(token);
        StudioSettings s = settings.findById(CurrentStudio.require()).orElseGet(StudioSettings::new);
        List<String> features = c.getServicePackage() == null ? List.of() : List.copyOf(c.getServicePackage().getFeatures());
        LocalDate eventDate = bookings.findFirstByContractIdAndStatusNotOrderByEventDateAsc(c.getId(), BookingStatus.CANCELLED)
                .map(Booking::getEventDate).orElse(null);
        return new PublicContractDto(c.getContractNumber(), new PublicStudioDto(s.getName(), s.getAddress(), s.getBankInfo()),
                c.getCustomerName(), mask(c.getPhone()), c.getPackageName(), features, c.getTotalAmount(), c.getPaidAmount(),
                c.getRemainingAmount(), ContractDocumentRenderer.installments(c.getTotalAmount()), eventDate, c.getStatus(),
                c.getAcceptedAt());
    }

    @Transactional(readOnly = true)
    public Document publicPdf(String token) {
        Contract c = byToken(token);
        return new Document(c.getContractNumber() + ".pdf",
                renderer.pdf(c, settings.findById(CurrentStudio.require()).orElseGet(StudioSettings::new)));
    }

    /** "Tôi đã đọc và đồng ý" — recorded once; pressing it again changes nothing */
    @Transactional
    public PublicContractDto accept(String token, String ip, String userAgent) {
        Contract c = byToken(token);
        if (c.getAcceptedAt() == null) {
            // PostgreSQL keeps microseconds: the first answer must match what later reads return
            c.setAcceptedAt(clock.instant().truncatedTo(ChronoUnit.MICROS));
            c.setAcceptedIp(ip);
            c.setAcceptedUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), 500)));
            notifications.notify("Khách đã đồng ý hợp đồng ✍️",
                    c.getCustomerName() + " đã xác nhận đồng ý hợp đồng " + c.getContractNumber() + " qua link Zalo.");
        }
        return publicView(token);
    }

    private Contract byToken(String token) {
        return contracts.findByPublicTokenHash(sha256(token))
                .filter(c -> c.getShareExpiresAt() != null && c.getShareExpiresAt().isAfter(clock.instant()))
                .orElseThrow(() -> new AppException(HttpStatus.GONE, "SHARE_LINK_EXPIRED",
                        "Link đã hết hạn, vui lòng liên hệ studio"));
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 0901112222 → 0901***222 */
    static String mask(String phone) {
        return phone.length() < 8 ? "***" : phone.substring(0, 4) + "***" + phone.substring(phone.length() - 3);
    }
}
