package com.weddingevent.common.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weddingevent.common.domain.enums.BookingStatus;
import com.weddingevent.common.exception.AppException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Booking state machine (spec §1.6, FN-ADM-CAL-04). */
class BookingStatusTest {

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @CsvSource({
            "UPCOMING, CONFIRMED, true",
            "UPCOMING, IN_PROGRESS, true",
            "CONFIRMED, IN_PROGRESS, true",
            "IN_PROGRESS, COMPLETED, true",
            "UPCOMING, COMPLETED, false",
            "CONFIRMED, COMPLETED, false",
            "CONFIRMED, UPCOMING, false",
            "IN_PROGRESS, CONFIRMED, false",
            "UPCOMING, UPCOMING, false",
            "COMPLETED, IN_PROGRESS, false",
            "CANCELLED, CONFIRMED, false",
            "UPCOMING, CANCELLED, true",
            "IN_PROGRESS, CANCELLED, true",
            "COMPLETED, CANCELLED, false",
    })
    void transitions(BookingStatus from, BookingStatus to, boolean allowed) {
        assertThat(from.canMoveTo(to)).isEqualTo(allowed);
    }

    @ParameterizedTest
    @CsvSource({"UPCOMING, COMPLETED", "COMPLETED, CONFIRMED"})
    void bookingRejectsInvalidMoves(BookingStatus from, BookingStatus to) {
        Booking b = new Booking();
        b.setStatus(from);
        assertThatThrownBy(() -> b.moveTo(to))
                .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getCode()).isEqualTo("INVALID_STATUS_TRANSITION"));
        assertThat(b.getStatus()).isEqualTo(from);
    }
}
