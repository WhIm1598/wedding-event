package com.weddingevent.common.domain;

import com.weddingevent.common.domain.enums.BookingStatus;
import com.weddingevent.common.exception.AppException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/** Studio appointment: photo shoot, dress fitting, ceremony coverage... (FN-ADM-CAL-01/02). */
@Getter
@Setter
@Entity
@Table(name = "bookings")
public class Booking extends BaseEntity {

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    private String phone;

    /** Appointment type label, e.g. "Chụp Pre-wedding" */
    @Column(name = "booking_type", nullable = false)
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_package_id")
    private ServicePackage servicePackage;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "event_time", nullable = false)
    private LocalTime eventTime;

    @Column(name = "venue_address")
    private String venueAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.UPCOMING;

    @ManyToMany
    @JoinTable(
            name = "booking_staff",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "staff_member_id"))
    private Set<StaffMember> staff = new LinkedHashSet<>();

    /** The customer this appointment is for; null only for legacy rows (FN-ADM-CRM-03) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    /** Set for the sessions of a sold service; consultations before signing have none */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    /** Rejects with 409 INVALID_STATUS_TRANSITION outside the §1.6 state machine. */
    public void moveTo(BookingStatus next) {
        if (!status.canMoveTo(next)) {
            throw AppException.conflict("INVALID_STATUS_TRANSITION",
                    "Không thể chuyển lịch hẹn từ trạng thái " + status + " sang " + next);
        }
        status = next;
    }
}
