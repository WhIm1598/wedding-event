package com.weddingevent.common.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weddingevent.common.domain.enums.ContractStatus;
import com.weddingevent.common.exception.AppException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Contract state machine (spec §1.6): paid in full is not completed; completed = paid in full AND delivered. */
class ContractTest {

    private static final Instant NOW = Instant.parse("2026-10-24T03:00:00Z");

    private static Contract contract(long total) {
        Contract c = new Contract();
        c.setTotalAmount(BigDecimal.valueOf(total));
        c.setRemainingAmount(BigDecimal.valueOf(total));
        return c;
    }

    @Test
    void firstPaymentMakesADraftEffective() {
        Contract c = contract(10_000_000);
        c.applyPayment(BigDecimal.valueOf(3_000_000));
        assertThat(c.getStatus()).isEqualTo(ContractStatus.DEPOSITED);
        assertThat(c.getPaidAmount()).isEqualByComparingTo("3000000");
        assertThat(c.getRemainingAmount()).isEqualByComparingTo("7000000");
        assertThat(c.isPaidInFull()).isFalse();
    }

    @Test
    void payingEverythingUpFrontDoesNotCompleteTheContract() {
        Contract c = contract(10_000_000);
        c.applyPayment(BigDecimal.valueOf(10_000_000));
        assertThat(c.isPaidInFull()).isTrue();
        assertThat(c.getStatus()).isEqualTo(ContractStatus.DEPOSITED);
    }

    @Test
    void workStartsOnlyOnAnEffectiveContract() {
        Contract draft = contract(10_000_000);
        draft.startWork();
        assertThat(draft.getStatus()).isEqualTo(ContractStatus.DRAFT);

        Contract deposited = contract(10_000_000);
        deposited.applyPayment(BigDecimal.ONE);
        deposited.startWork();
        assertThat(deposited.getStatus()).isEqualTo(ContractStatus.IN_PROGRESS);
        deposited.startWork(); // later bookings change nothing
        assertThat(deposited.getStatus()).isEqualTo(ContractStatus.IN_PROGRESS);
    }

    @Test
    void deliveringAPaidContractCompletesIt() {
        Contract c = contract(10_000_000);
        c.applyPayment(BigDecimal.valueOf(10_000_000));
        c.startWork();
        c.deliver(NOW);
        assertThat(c.getDeliveredAt()).isEqualTo(NOW);
        assertThat(c.getStatus()).isEqualTo(ContractStatus.COMPLETED);
    }

    @Test
    void deliveredWithDebtCompletesOnTheLastPayment() {
        Contract c = contract(10_000_000);
        c.applyPayment(BigDecimal.valueOf(4_000_000));
        c.startWork();
        c.deliver(NOW);
        assertThat(c.getStatus()).isEqualTo(ContractStatus.IN_PROGRESS);
        c.applyPayment(BigDecimal.valueOf(6_000_000));
        assertThat(c.getStatus()).isEqualTo(ContractStatus.COMPLETED);
    }

    @Test
    void onlyAnEffectiveContractCanBeDeliveredAndOnlyOnce() {
        Contract draft = contract(10_000_000);
        assertThatThrownBy(() -> draft.deliver(NOW))
                .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getCode()).isEqualTo("INVALID_STATUS_TRANSITION"));

        draft.applyPayment(BigDecimal.ONE);
        draft.deliver(NOW);
        assertThatThrownBy(() -> draft.deliver(NOW)).isInstanceOf(AppException.class);
    }

    @Test
    void aRentalWithoutBookingsIsDeliveredStraightFromDeposited() {
        Contract rental = contract(3_000_000);
        rental.applyPayment(BigDecimal.valueOf(3_000_000));
        assertThat(rental.getStatus()).isEqualTo(ContractStatus.DEPOSITED);
        rental.deliver(NOW);
        assertThat(rental.getStatus()).isEqualTo(ContractStatus.COMPLETED);
    }
}
