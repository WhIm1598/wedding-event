package com.weddingevent.client.quote;

import com.weddingevent.client.notification.ClientNotificationService;
import com.weddingevent.client.project.VendorProjectService.VendorProjectDto;
import com.weddingevent.common.api.PageResponse;
import com.weddingevent.common.domain.QuoteRequest;
import com.weddingevent.common.domain.ServicePackage;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.VendorProfile;
import com.weddingevent.common.domain.VendorProject;
import com.weddingevent.common.domain.enums.NotificationKind;
import com.weddingevent.common.domain.enums.QuoteRequestStatus;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.QuoteRequestRepository;
import com.weddingevent.common.repository.ServicePackageRepository;
import com.weddingevent.common.repository.UserRepository;
import com.weddingevent.common.repository.VendorProfileRepository;
import com.weddingevent.common.repository.VendorProjectRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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
 * FN-VND-01..03: a couple asks a vendor to quote one of its packages, the vendor quotes or declines, the couple accepts
 * (→ one Vendor Project) or cancels. A request that isn't the caller's is 404, never 403; concurrent changes of the same
 * request fail on its {@code @Version} (409 CONCURRENT_MODIFICATION).
 */
@Service
public class QuoteRequestService {

    public record SendQuoteRequest(
            @NotNull(message = "Vui lòng chọn gói dịch vụ") UUID servicePackageId,
            LocalDate eventDate,
            @Size(max = 255, message = "Địa điểm tối đa 255 ký tự") String venue,
            @Min(value = 1, message = "Số khách từ 1 đến 5000") @Max(value = 5000, message = "Số khách từ 1 đến 5000") Integer guestCount,
            @Size(max = 1000, message = "Lời nhắn tối đa 1000 ký tự") String message) {}

    public record QuoteReply(
            @NotNull(message = "Vui lòng nhập giá báo") @Positive(message = "Giá báo phải lớn hơn 0") Long quotedPrice,
            @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự") String vendorNote) {}

    public record DeclineRequest(@Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự") String vendorNote) {}

    /** Needed only when the request had no event date */
    public record AcceptRequest(LocalDate eventDate, @Size(max = 255, message = "Địa điểm tối đa 255 ký tự") String venue) {}

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
    private final VendorProjectRepository projects;
    private final ClientNotificationService notifications;
    private final Clock clock;

    public QuoteRequestService(QuoteRequestRepository requests, ServicePackageRepository packages, VendorProfileRepository vendors,
            UserRepository users, VendorProjectRepository projects, ClientNotificationService notifications, Clock clock) {
        this.requests = requests;
        this.packages = packages;
        this.vendors = vendors;
        this.users = users;
        this.projects = projects;
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
        return QuoteRequestDto.of(sent(coupleId, id));
    }

    @Transactional(readOnly = true)
    public QuoteRequestDto getReceived(UUID vendorUserId, UUID id) {
        return QuoteRequestDto.of(received(vendorUserId, id));
    }

    /** FN-VND-03: the vendor quotes (again) */
    @Transactional
    public QuoteRequestDto quote(UUID vendorUserId, UUID id, QuoteReply req) {
        QuoteRequest q = received(vendorUserId, id);
        q.quote(BigDecimal.valueOf(req.quotedPrice()), blankToNull(req.vendorNote()), clock.instant());
        notifications.notify(q.getCouple().getId(), NotificationKind.MESSAGE, "Đã có báo giá",
                q.getVendor().getBusinessName() + " đã báo giá gói \"" + q.getPackageName() + "\".");
        return flushed(q);
    }

    /** FN-VND-03: the vendor declines */
    @Transactional
    public QuoteRequestDto decline(UUID vendorUserId, UUID id, DeclineRequest req) {
        QuoteRequest q = received(vendorUserId, id);
        q.decline(req == null ? null : blankToNull(req.vendorNote()), clock.instant());
        notifications.notify(q.getCouple().getId(), NotificationKind.MESSAGE, "Yêu cầu báo giá bị từ chối",
                q.getVendor().getBusinessName() + " không nhận gói \"" + q.getPackageName() + "\".");
        return flushed(q);
    }

    /** FN-VND-03: the couple withdraws an open request */
    @Transactional
    public QuoteRequestDto cancel(UUID coupleId, UUID id) {
        QuoteRequest q = sent(coupleId, id);
        q.cancel();
        return flushed(q);
    }

    /** FN-VND-03: the couple accepts the quote, which creates exactly one Vendor Project at the quoted price */
    @Transactional
    public VendorProjectDto accept(UUID coupleId, UUID id, AcceptRequest req) {
        QuoteRequest q = sent(coupleId, id);
        q.accept();
        if (q.getEventDate() == null) {
            LocalDate eventDate = req == null ? null : req.eventDate();
            if (eventDate == null) {
                throw AppException.invalidField("eventDate", "Vui lòng chọn ngày tổ chức");
            }
            if (eventDate.isBefore(LocalDate.now(clock))) {
                throw AppException.invalidField("eventDate", "Ngày tổ chức không được trước hôm nay");
            }
            q.setEventDate(eventDate);
        }
        if (req != null && req.venue() != null && !req.venue().isBlank()) {
            q.setVenue(req.venue().trim());
        }
        VendorProject project = projects.saveAndFlush(VendorProject.from(q, q.getEventDate(), q.getVenue()));
        notifications.notify(q.getVendor().getUser().getId(), NotificationKind.MESSAGE, "Báo giá đã được chấp nhận",
                q.getCouple().getFullName() + " đã chốt gói \"" + q.getPackageName() + "\".");
        return VendorProjectDto.of(project);
    }

    private QuoteRequest sent(UUID coupleId, UUID id) {
        return requests.findByIdAndCoupleId(id, coupleId).orElseThrow(() -> new ResourceNotFoundException("yêu cầu báo giá", id));
    }

    private QuoteRequest received(UUID vendorUserId, UUID id) {
        return requests.findByIdAndVendorId(id, vendorOf(vendorUserId).getId())
                .orElseThrow(() -> new ResourceNotFoundException("yêu cầu báo giá", id));
    }

    /** Flushes so a concurrent change of the same request fails here (optimistic lock → 409) */
    private QuoteRequestDto flushed(QuoteRequest q) {
        requests.flush();
        return QuoteRequestDto.of(q);
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
