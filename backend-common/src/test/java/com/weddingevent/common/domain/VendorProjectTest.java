package com.weddingevent.common.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weddingevent.common.domain.enums.VendorProjectStatus;
import com.weddingevent.common.exception.AppException;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

/** VendorProjectStatus transitions and the couple's cancellation request (spec §1.6, FN-VND-04) */
class VendorProjectTest {

    private static final Instant NOW = Instant.parse("2026-10-26T03:00:00Z");

    private static VendorProject project(VendorProjectStatus status) {
        VendorProject p = new VendorProject();
        p.setStatus(status);
        return p;
    }

    private static void assertRefused(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getCode()).isEqualTo("INVALID_STATUS_TRANSITION"));
    }

    @ParameterizedTest
    @CsvSource({
        "UPCOMING, IN_PROGRESS, true", "UPCOMING, COMPLETED, true", "UPCOMING, CANCELLED, true", "UPCOMING, UPCOMING, false",
        "IN_PROGRESS, COMPLETED, true", "IN_PROGRESS, CANCELLED, true", "IN_PROGRESS, UPCOMING, false",
        "COMPLETED, CANCELLED, false", "COMPLETED, IN_PROGRESS, false", "CANCELLED, UPCOMING, false", "CANCELLED, COMPLETED, false"
    })
    void statusMachine(VendorProjectStatus from, VendorProjectStatus to, boolean allowed) {
        VendorProject p = project(from);
        if (allowed) {
            p.moveTo(to);
            assertThat(p.getStatus()).isEqualTo(to);
        } else {
            assertRefused(() -> p.moveTo(to));
        }
    }

    @Test
    void theCoupleAsksToCancelAndTheVendorApproves() {
        VendorProject p = project(VendorProjectStatus.IN_PROGRESS);
        p.requestCancel("Đổi địa điểm", NOW);
        assertThat(p.hasPendingCancelRequest()).isTrue();
        assertThat(p.getStatus()).isEqualTo(VendorProjectStatus.IN_PROGRESS); // unchanged while waiting
        assertRefused(() -> p.requestCancel("Lần nữa", NOW)); // one request at a time

        p.approveCancel();
        assertThat(p.getStatus()).isEqualTo(VendorProjectStatus.CANCELLED);
        assertThat(p.hasPendingCancelRequest()).isFalse();
        assertThat(p.getCancelReason()).isEqualTo("Đổi địa điểm");
    }

    @Test
    void theVendorRejectsAndTheProjectGoesOn() {
        VendorProject p = project(VendorProjectStatus.UPCOMING);
        assertRefused(p::approveCancel);
        assertRefused(p::rejectCancel);

        p.requestCancel("Đổi ý", NOW);
        p.rejectCancel();
        assertThat(p.hasPendingCancelRequest()).isFalse();
        assertThat(p.getCancelRequestedAt()).isNull();
        assertThat(p.getStatus()).isEqualTo(VendorProjectStatus.UPCOMING);
        p.requestCancel("Đổi ý lần 2", NOW); // may ask again
    }

    @Test
    void movingTheWorkOnRejectsAPendingRequest() {
        VendorProject p = project(VendorProjectStatus.UPCOMING);
        p.requestCancel("Đổi ý", NOW);
        assertThat(p.moveTo(VendorProjectStatus.IN_PROGRESS)).isTrue(); // tells the caller a request was rejected
        assertThat(p.hasPendingCancelRequest()).isFalse();
        assertThat(p.moveTo(VendorProjectStatus.COMPLETED)).isFalse();
    }

    @Test
    void finishedProjectsCannotBeCancelled() {
        assertRefused(() -> project(VendorProjectStatus.COMPLETED).requestCancel("x", NOW));
        assertRefused(() -> project(VendorProjectStatus.CANCELLED).requestCancel("x", NOW));
    }
}
