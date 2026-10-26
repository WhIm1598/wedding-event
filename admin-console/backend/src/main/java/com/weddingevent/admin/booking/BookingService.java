package com.weddingevent.admin.booking;

import com.weddingevent.admin.notification.NotificationService;
import com.weddingevent.admin.security.CurrentStudio;
import com.weddingevent.admin.security.CurrentUser;
import com.weddingevent.admin.staff.StaffService;
import com.weddingevent.admin.support.DateRange;
import com.weddingevent.common.domain.Booking;
import com.weddingevent.common.domain.Contract;
import com.weddingevent.common.domain.Lead;
import com.weddingevent.common.domain.ServicePackage;
import com.weddingevent.common.domain.StaffMember;
import com.weddingevent.common.domain.enums.BookingStatus;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.BookingRepository;
import com.weddingevent.common.repository.ContractRepository;
import com.weddingevent.common.repository.LeadRepository;
import com.weddingevent.common.repository.ServicePackageRepository;
import com.weddingevent.common.repository.StaffRepository;
import com.weddingevent.common.util.VndFormatter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Smart booking calendar (FN-ADM-CAL-01 / 02 / 04). */
@Service
public class BookingService {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    public enum CalendarView { DAY, WEEK, MONTH }

    public record StaffRefDto(String id, String name, String role) {}

    public record BookingDto(
            String id, LocalDate date, String time, String clientName, String phone, String type,
            String packageName, String venueAddress, BookingStatus status, List<StaffRefDto> assignedStaff, String contractId) {

        static BookingDto of(Booking b) {
            List<StaffRefDto> staff = b.getStaff().stream()
                    .sorted(Comparator.comparing(StaffMember::getCode))
                    .map(s -> new StaffRefDto(s.getId().toString(), s.getFullName(), s.getPosition()))
                    .toList();
            return new BookingDto(
                    b.getId().toString(), b.getEventDate(), b.getEventTime().format(HH_MM), b.getCustomerName(), b.getPhone(),
                    b.getType(), b.getServicePackage() == null ? "—" : b.getServicePackage().getName(), b.getVenueAddress(),
                    b.getStatus(), staff, b.getContract() == null ? null : b.getContract().getId().toString());
        }
    }

    /** {@code contractId} / {@code leadId} (v1.1, FN-ADM-CRM-03): a contract's phone and package fill the blanks. */
    public record CreateBookingRequest(
            @NotBlank @Size(max = 100) String clientName,
            @Pattern(regexp = "^$|" + StaffService.VN_PHONE, message = "Số điện thoại không hợp lệ") String phone,
            UUID servicePackageId,
            @NotBlank @Size(max = 100) String type,
            @NotNull LocalDate eventDate,
            @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Giờ hẹn phải có dạng HH:mm") String eventTime,
            @Size(max = 255) String venueAddress,
            List<UUID> staffIds,
            UUID contractId,
            UUID leadId) {}

    /** FN-ADM-CAL-04; cancelling goes through its own endpoint (FN-ADM-CAL-03) */
    public record UpdateStatusRequest(@NotNull BookingStatus status) {}

    private final BookingRepository bookings;
    private final StaffRepository staff;
    private final ServicePackageRepository packages;
    private final ContractRepository contracts;
    private final LeadRepository leads;
    private final NotificationService notifications;
    private final Clock clock;

    public BookingService(BookingRepository bookings, StaffRepository staff, ServicePackageRepository packages,
            ContractRepository contracts, LeadRepository leads, NotificationService notifications, Clock clock) {
        this.bookings = bookings;
        this.staff = staff;
        this.packages = packages;
        this.contracts = contracts;
        this.leads = leads;
        this.notifications = notifications;
        this.clock = clock;
    }

    /**
     * ROLE_STAFF only sees bookings they are assigned to, regardless of the staffId filter.
     * Cancelled bookings are hidden unless {@code includeCancelled} is set.
     */
    @Transactional(readOnly = true)
    public List<BookingDto> calendar(CalendarView view, LocalDate date, UUID staffId, boolean includeCancelled, CurrentUser user) {
        DateRange range = switch (view) {
            case DAY -> DateRange.day(date);
            case WEEK -> DateRange.week(date);
            case MONTH -> DateRange.month(date);
        };
        UUID effectiveStaff = user.isAdmin() ? staffId : user.staffMemberId();
        if (!user.isAdmin() && effectiveStaff == null) {
            return List.of();
        }
        List<Booking> result = effectiveStaff == null
                ? bookings.findByEventDateBetweenOrderByEventDateAscEventTimeAsc(range.from(), range.to())
                : bookings.findForStaffBetween(effectiveStaff, range.from(), range.to());
        return result.stream()
                .filter(b -> includeCancelled || b.getStatus() != BookingStatus.CANCELLED)
                .map(BookingDto::of)
                .toList();
    }

    /** Rejects with 409 STAFF_SCHEDULE_CONFLICT when an assigned staff member is already booked at that slot. */
    @Transactional
    public BookingDto create(CreateBookingRequest req) {
        if (req.eventDate().isBefore(LocalDate.now(clock))) {
            throw AppException.badRequest("BAD_REQUEST", "Ngày hẹn không được ở trong quá khứ");
        }
        LocalTime time = LocalTime.parse(req.eventTime(), HH_MM);
        List<UUID> staffIds = req.staffIds() == null ? List.of() : List.copyOf(new HashSet<>(req.staffIds()));
        List<StaffMember> assigned = staff.findAllByIdForUpdate(staffIds);
        if (assigned.size() != staffIds.size()) {
            throw AppException.badRequest("STAFF_NOT_FOUND", "Có nhân sự không tồn tại trong danh sách phân công");
        }
        if (!staffIds.isEmpty()) {
            List<String> busy = bookings.findBusyStaffNames(req.eventDate(), time, staffIds, BookingStatus.CANCELLED);
            if (!busy.isEmpty()) {
                throw AppException.conflict("STAFF_SCHEDULE_CONFLICT",
                        "Nhân sự " + String.join(", ", busy) + " đã có lịch vào khung giờ này");
            }
        }
        Contract contract = req.contractId() == null ? null
                : contracts.findById(req.contractId()).orElseThrow(() -> new ResourceNotFoundException("hợp đồng", req.contractId()));
        Lead lead = req.leadId() == null ? null
                : leads.findById(req.leadId()).orElseThrow(() -> new ResourceNotFoundException("khách hàng", req.leadId()));
        if (contract != null) {
            if (lead != null && contract.getLead() != null && !contract.getLead().getId().equals(lead.getId())) {
                throw AppException.invalidField("leadId", "Khách hàng không phải là khách của hợp đồng này");
            }
            if (contract.getLead() != null) {
                lead = contract.getLead();
            }
        }
        ServicePackage pkg = req.servicePackageId() == null ? (contract == null ? null : contract.getServicePackage())
                : packages.findByIdAndStudioId(req.servicePackageId(), CurrentStudio.require()).orElseThrow(() -> new ResourceNotFoundException("gói dịch vụ", req.servicePackageId()));
        String phone = req.phone() == null || req.phone().isBlank() ? (contract == null ? null : contract.getPhone()) : req.phone();

        Booking b = new Booking();
        b.setContract(contract);
        b.setLead(lead);
        b.setCustomerName(req.clientName().trim());
        b.setPhone(phone);
        b.setType(req.type().trim());
        b.setServicePackage(pkg);
        b.setEventDate(req.eventDate());
        b.setEventTime(time);
        b.setVenueAddress(req.venueAddress() == null || req.venueAddress().isBlank() ? null : req.venueAddress().trim());
        b.getStaff().addAll(assigned);
        b = bookings.save(b);
        notifications.notify("Lịch hẹn mới 📅", b.getCustomerName() + " - " + b.getType() + " lúc " + req.eventTime()
                + " ngày " + VndFormatter.date(b.getEventDate()) + ".");
        return BookingDto.of(b);
    }

    /**
     * Execution status (FN-ADM-CAL-04) along the §1.6 state machine. Staff may only move bookings they are assigned
     * to (403). The first booking of an effective contract going IN_PROGRESS starts the contract.
     */
    @Transactional
    public BookingDto updateStatus(UUID id, BookingStatus next, CurrentUser user) {
        if (next == BookingStatus.UPCOMING || next == BookingStatus.CANCELLED) {
            throw AppException.invalidField("status", "Trạng thái phải là CONFIRMED, IN_PROGRESS hoặc COMPLETED");
        }
        Booking b = bookings.findById(id).orElseThrow(() -> new ResourceNotFoundException("lịch hẹn", id));
        if (!user.isAdmin() && b.getStaff().stream().noneMatch(s -> s.getId().equals(user.staffMemberId()))) {
            throw AppException.forbidden("FORBIDDEN", "Bạn chỉ cập nhật được lịch hẹn được phân công cho mình");
        }
        b.moveTo(next);
        if (next == BookingStatus.IN_PROGRESS && b.getContract() != null) {
            b.getContract().startWork();
        }
        return BookingDto.of(b);
    }
}
