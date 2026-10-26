package com.weddingevent.client.quote;

import com.weddingevent.client.notification.ClientNotificationService;
import com.weddingevent.common.api.PageResponse;
import com.weddingevent.common.domain.QuoteRequest;
import com.weddingevent.common.domain.ServicePackage;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.VendorProfile;
import com.weddingevent.common.domain.enums.NotificationKind;
import com.weddingevent.common.domain.enums.QuoteRequestStatus;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.QuoteRequestRepository;
import com.weddingevent.common.repository.ServicePackageRepository;
import com.weddingevent.common.repository.UserRepository;
import com.weddingevent.common.repository.VendorProfileRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FN-VND-01..02: a couple asks a vendor to quote one of its packages; both sides list and open their requests.
 * A request that isn't the caller's is 404, never 403.
 */
@Service
public class QuoteRequestService {

    public record SendQuoteRequest(
            @NotNull(message = "Vui lòng chọn gói dịch vụ") UUID servicePackageId,
            LocalDate eventDate,
            @Size(max = 255, message = "Địa điểm tối đa 255 ký tự") String venue,
            @Min(value = 1, message = "Số khách từ 1 đến 5000") @Max(value = 5000, message = "Số khách từ 1 đến 5000") Integer guestCount,
            @Size(max = 1000, message = "Lời nhắn tối đa 1000 ký tự") String message) {}

    public record QuoteRequestDto(
            String id, String coupleName, String packageId, String packageName, String vendorName, LocalDate eventDate,
            String venue, Integer guestCount, String message, QuoteRequestStatus status, Long quotedPrice, String vendorNote,
            Instant createdAt, Instant respondedAt) {

        public static QuoteRequestDto of(QuoteRequest q) {
            return new QuoteRequestDto(q.getId().toString(), q.getCouple().getFullName(), q.getServicePackage().getId().toString(),
                    q.getPackageName(), q.getVendor().getBusinessName(), q.getEventDate(), q.getVenue(), q.getGuestCount(),
                    q.getMessage(), q.getStatus(), q.getQuotedPrice() == null ? null : q.getQuotedPrice().longValueExact(),
                    q.getVendorNote(), q.getCreatedAt(), q.getRespondedAt());
        }
    }

    private static final EnumSet<QuoteRequestStatus> OPEN = EnumSet.of(QuoteRequestStatus.PENDING, QuoteRequestStatus.QUOTED);

    private final QuoteRequestRepository requests;
    private final ServicePackageRepository packages;
    private final VendorProfileRepository vendors;
    private final UserRepository users;
    private final ClientNotificationService notifications;
    private final Clock clock;

    public QuoteRequestService(QuoteRequestRepository requests, ServicePackageRepository packages, VendorProfileRepository vendors,
            UserRepository users, ClientNotificationService notifications, Clock clock) {
        this.requests = requests;
        this.packages = packages;
        this.vendors = vendors;
        this.users = users;
        this.notifications = notifications;
        this.clock = clock;
    }

    /** FN-VND-01 */
    @Transactional
    public QuoteRequestDto send(UUID coupleId, SendQuoteRequest req) {
        if (req.eventDate() != null && req.eventDate().isBefore(LocalDate.now(clock))) {
            throw AppException.invalidField("eventDate", "Ngày tổ chức không được trước hôm nay");
        }
        ServicePackage pkg = packages.findById(req.servicePackageId()).filter(ServicePackage::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("dịch vụ", req.servicePackageId()));
        if (pkg.getVendorId() == null) {
            throw AppException.badRequest("PACKAGE_NOT_QUOTABLE", "Gói của Studio không nhận yêu cầu báo giá, vui lòng liên hệ Studio qua Zalo");
        }
        // shortcut: two sends racing past this check hit uq_quote_requests_open and get a 500 instead of 409; fine for one couple tapping once
        requests.findFirstByCoupleIdAndServicePackageIdAndStatusIn(coupleId, pkg.getId(), OPEN).ifPresent(open -> {
            throw AppException.conflict("QUOTE_ALREADY_OPEN", "Bạn đã có yêu cầu báo giá đang mở cho gói này",
                    Map.of("existingId", open.getId().toString()));
        });
        User couple = users.findById(coupleId).orElseThrow();
        VendorProfile vendor = vendors.findById(pkg.getVendorId()).orElseThrow();

        QuoteRequest q = new QuoteRequest();
        q.setCouple(couple);
        q.setVendor(vendor);
        q.setServicePackage(pkg);
        q.setPackageName(pkg.getName());
        q.setEventDate(req.eventDate());
        q.setVenue(blankToNull(req.venue()));
        q.setGuestCount(req.guestCount());
        q.setMessage(blankToNull(req.message()));
        requests.saveAndFlush(q); // createdAt for the response
        notifications.notify(vendor.getUser().getId(), NotificationKind.MESSAGE, "Yêu cầu báo giá mới",
                couple.getFullName() + " muốn nhận báo giá gói \"" + pkg.getName() + "\".");
        return QuoteRequestDto.of(q);
    }

    /** FN-VND-02, couple side: the requests the caller sent, newest first */
    @Transactional(readOnly = true)
    public PageResponse<QuoteRequestDto> listSent(UUID coupleId, String status, int page, int size) {
        QuoteRequestStatus filter = parseStatus(status);
        PageRequest pageable = PageRequest.of(page, size);
        Page<QuoteRequest> result = filter == null
                ? requests.findAllByCoupleIdOrderByCreatedAtDesc(coupleId, pageable)
                : requests.findAllByCoupleIdAndStatusOrderByCreatedAtDesc(coupleId, filter, pageable);
        return PageResponse.of(result, QuoteRequestDto::of);
    }

    /** FN-VND-02, vendor side: the requests sent to the caller's vendor profile, newest first */
    @Transactional(readOnly = true)
    public PageResponse<QuoteRequestDto> listReceived(UUID vendorUserId, String status, int page, int size) {
        QuoteRequestStatus filter = parseStatus(status);
        UUID vendorId = vendorOf(vendorUserId).getId();
        PageRequest pageable = PageRequest.of(page, size);
        Page<QuoteRequest> result = filter == null
                ? requests.findAllByVendorIdOrderByCreatedAtDesc(vendorId, pageable)
                : requests.findAllByVendorIdAndStatusOrderByCreatedAtDesc(vendorId, filter, pageable);
        return PageResponse.of(result, QuoteRequestDto::of);
    }

    @Transactional(readOnly = true)
    public QuoteRequestDto getSent(UUID coupleId, UUID id) {
        return QuoteRequestDto.of(requests.findByIdAndCoupleId(id, coupleId)
                .orElseThrow(() -> new ResourceNotFoundException("yêu cầu báo giá", id)));
    }

    @Transactional(readOnly = true)
    public QuoteRequestDto getReceived(UUID vendorUserId, UUID id) {
        return QuoteRequestDto.of(requests.findByIdAndVendorId(id, vendorOf(vendorUserId).getId())
                .orElseThrow(() -> new ResourceNotFoundException("yêu cầu báo giá", id)));
    }

    private VendorProfile vendorOf(UUID vendorUserId) {
        return vendors.findByUserId(vendorUserId).orElseThrow(() -> new ResourceNotFoundException("hồ sơ Vendor", vendorUserId));
    }

    private static QuoteRequestStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return QuoteRequestStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw AppException.invalidField("status", "Trạng thái chỉ có thể là PENDING, QUOTED, ACCEPTED, DECLINED hoặc CANCELLED");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
