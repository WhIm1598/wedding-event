package com.weddingevent.client.project;

import com.weddingevent.client.notification.ClientNotificationService;
import com.weddingevent.common.api.PageResponse;
import com.weddingevent.common.domain.VendorProject;
import com.weddingevent.common.domain.enums.NotificationKind;
import com.weddingevent.common.domain.enums.VendorProjectStatus;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.VendorProfileRepository;
import com.weddingevent.common.repository.VendorProjectRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FN-VND-04: Vendor Projects (created when a couple accepts a quote, FN-VND-03). The vendor moves them along §1.6;
 * the couple may ask to cancel and the vendor approves or rejects. Not the caller's project → 404.
 */
@Service
public class VendorProjectService {

    public record VendorProjectDto(
            String id, String coupleName, String vendorName, String packageName, LocalDate eventDate, String venue, long agreedPrice,
            VendorProjectStatus status, String quoteRequestId, Instant cancelRequestedAt, String cancelReason) {

        public static VendorProjectDto of(VendorProject p) {
            return new VendorProjectDto(p.getId().toString(), p.getCoupleName(), p.getVendor().getBusinessName(), p.getPackageName(), p.getEventDate(), p.getVenue(),
                    p.getAgreedPrice().longValueExact(), p.getStatus(), p.getQuoteRequest().getId().toString(),
                    p.hasPendingCancelRequest() ? p.getCancelRequestedAt() : null, p.getCancelReason());
        }
    }

    public record UpdateStatusRequest(@NotNull(message = "Vui lòng chọn trạng thái") VendorProjectStatus status) {}

    public record CancelRequest(
            @NotBlank(message = "Vui lòng nhập lý do hủy") @Size(max = 500, message = "Lý do tối đa 500 ký tự") String reason) {}

    private final VendorProjectRepository projects;
    private final VendorProfileRepository vendors;
    private final ClientNotificationService notifications;
    private final Clock clock;

    public VendorProjectService(VendorProjectRepository projects, VendorProfileRepository vendors,
            ClientNotificationService notifications, Clock clock) {
        this.projects = projects;
        this.vendors = vendors;
        this.notifications = notifications;
        this.clock = clock;
    }

    /** Vendor: by event date; finished projects (COMPLETED/CANCELLED) most recent first */
    @Transactional(readOnly = true)
    public PageResponse<VendorProjectDto> listForVendor(UUID vendorUserId, String status, int page, int size) {
        VendorProjectStatus filter = parseStatus(status);
        UUID vendorId = vendorId(vendorUserId);
        PageRequest pageable = PageRequest.of(page, size);
        Page<VendorProject> result;
        if (filter == null) {
            result = projects.findAllByVendorIdOrderByEventDateAsc(vendorId, pageable);
        } else if (filter == VendorProjectStatus.COMPLETED || filter == VendorProjectStatus.CANCELLED) {
            result = projects.findAllByVendorIdAndStatusOrderByEventDateDesc(vendorId, filter, pageable);
        } else {
            result = projects.findAllByVendorIdAndStatusOrderByEventDateAsc(vendorId, filter, pageable);
        }
        return PageResponse.of(result, VendorProjectDto::of);
    }

    /** Couple: the vendors they have booked, by event date */
    @Transactional(readOnly = true)
    public PageResponse<VendorProjectDto> listForCouple(UUID coupleId, int page, int size) {
        return PageResponse.of(projects.findAllByCoupleIdOrderByEventDateAsc(coupleId, PageRequest.of(page, size)), VendorProjectDto::of);
    }

    @Transactional
    public VendorProjectDto updateStatus(UUID vendorUserId, UUID id, VendorProjectStatus next) {
        VendorProject p = ofVendor(vendorUserId, id);
        boolean rejectedCancel = p.moveTo(next);
        if (rejectedCancel) {
            notifyCouple(p, "Yêu cầu hủy dự án bị từ chối", p.getVendor().getBusinessName() + " vẫn tiếp tục dự án \"" + p.getPackageName() + "\".");
        }
        if (next == VendorProjectStatus.COMPLETED) {
            notifyCouple(p, "Dự án đã hoàn thành", p.getVendor().getBusinessName() + " đã hoàn thành \"" + p.getPackageName() + "\".");
        } else if (next == VendorProjectStatus.CANCELLED) {
            notifyCouple(p, "Dự án đã được hủy", p.getVendor().getBusinessName() + " đã hủy \"" + p.getPackageName() + "\".");
        }
        return flushed(p);
    }

    @Transactional
    public VendorProjectDto requestCancel(UUID coupleId, UUID id, CancelRequest req) {
        VendorProject p = projects.findByIdAndCoupleId(id, coupleId).orElseThrow(() -> new ResourceNotFoundException("dự án", id));
        p.requestCancel(req.reason().trim(), clock.instant());
        notifications.notify(p.getVendor().getUser().getId(), NotificationKind.MESSAGE, "Cặp đôi xin hủy dự án",
                p.getCoupleName() + " xin hủy \"" + p.getPackageName() + "\": " + p.getCancelReason());
        return flushed(p);
    }

    @Transactional
    public VendorProjectDto approveCancel(UUID vendorUserId, UUID id) {
        VendorProject p = ofVendor(vendorUserId, id);
        p.approveCancel();
        notifyCouple(p, "Dự án đã được hủy", p.getVendor().getBusinessName() + " đã đồng ý hủy \"" + p.getPackageName() + "\".");
        return flushed(p);
    }

    @Transactional
    public VendorProjectDto rejectCancel(UUID vendorUserId, UUID id) {
        VendorProject p = ofVendor(vendorUserId, id);
        p.rejectCancel();
        notifyCouple(p, "Yêu cầu hủy dự án bị từ chối", p.getVendor().getBusinessName() + " vẫn tiếp tục dự án \"" + p.getPackageName() + "\".");
        return flushed(p);
    }

    private void notifyCouple(VendorProject p, String title, String body) {
        notifications.notify(p.getCouple().getId(), NotificationKind.MESSAGE, title, body);
    }

    private VendorProject ofVendor(UUID vendorUserId, UUID id) {
        return projects.findByIdAndVendorId(id, vendorId(vendorUserId)).orElseThrow(() -> new ResourceNotFoundException("dự án", id));
    }

    private UUID vendorId(UUID vendorUserId) {
        return vendors.findByUserId(vendorUserId).orElseThrow(() -> new ResourceNotFoundException("hồ sơ Vendor", vendorUserId)).getId();
    }

    /** Flushes so a concurrent change of the same project fails here (optimistic lock → 409) */
    private VendorProjectDto flushed(VendorProject p) {
        projects.flush();
        return VendorProjectDto.of(p);
    }

    private static VendorProjectStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return VendorProjectStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw AppException.invalidField("status", "Trạng thái chỉ có thể là UPCOMING, IN_PROGRESS, COMPLETED hoặc CANCELLED");
        }
    }
}
