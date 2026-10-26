package com.weddingevent.common.domain.enums;

/** {@code UPCOMING} = waiting for confirmation ("Chờ xác nhận", GLOSSARY: Pending Booking), not a date filter. */
public enum BookingStatus {
    UPCOMING, IN_PROGRESS, CONFIRMED, COMPLETED, CANCELLED;

    /** State machine of spec §1.6: UPCOMING → CONFIRMED → IN_PROGRESS → COMPLETED, UPCOMING → IN_PROGRESS, any open → CANCELLED */
    public boolean canMoveTo(BookingStatus next) {
        return switch (this) {
            case UPCOMING -> next == CONFIRMED || next == IN_PROGRESS || next == CANCELLED;
            case CONFIRMED -> next == IN_PROGRESS || next == CANCELLED;
            case IN_PROGRESS -> next == COMPLETED || next == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
