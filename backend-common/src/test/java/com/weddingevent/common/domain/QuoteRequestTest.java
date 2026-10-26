package com.weddingevent.common.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weddingevent.common.domain.enums.QuoteRequestStatus;
import com.weddingevent.common.exception.AppException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** QuoteRequestStatus transitions (spec §1.6, FN-VND-03) */
class QuoteRequestTest {

    private static final Instant NOW = Instant.parse("2026-10-26T03:00:00Z");

    private static void assertRefused(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getCode()).isEqualTo("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void theVendorQuotesAndMayQuoteAgain() {
        QuoteRequest q = new QuoteRequest();
        q.quote(BigDecimal.valueOf(18_000_000), "Đã gồm vận chuyển", NOW);
        assertThat(q.getStatus()).isEqualTo(QuoteRequestStatus.QUOTED);
        assertThat(q.getRespondedAt()).isEqualTo(NOW);

        q.quote(BigDecimal.valueOf(16_000_000), null, NOW.plusSeconds(60));
        assertThat(q.getQuotedPrice()).isEqualByComparingTo("16000000");
        assertThat(q.getVendorNote()).isNull();
        assertThat(q.getRespondedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void onlyAQuotedRequestCanBeAccepted() {
        QuoteRequest q = new QuoteRequest();
        assertRefused(q::accept);
        q.quote(BigDecimal.TEN, null, NOW);
        q.accept();
        assertThat(q.getStatus()).isEqualTo(QuoteRequestStatus.ACCEPTED);
    }

    @Test
    void openRequestsCanBeDeclinedOrCancelled() {
        QuoteRequest pending = new QuoteRequest();
        pending.decline("Kín lịch", NOW);
        assertThat(pending.getStatus()).isEqualTo(QuoteRequestStatus.DECLINED);
        assertThat(pending.getVendorNote()).isEqualTo("Kín lịch");

        QuoteRequest quoted = new QuoteRequest();
        quoted.quote(BigDecimal.TEN, null, NOW);
        quoted.cancel();
        assertThat(quoted.getStatus()).isEqualTo(QuoteRequestStatus.CANCELLED);
    }

    @Test
    void finalStatesStayFinal() {
        QuoteRequest accepted = new QuoteRequest();
        accepted.quote(BigDecimal.TEN, null, NOW);
        accepted.accept();
        QuoteRequest declined = new QuoteRequest();
        declined.decline(null, NOW);
        QuoteRequest cancelled = new QuoteRequest();
        cancelled.cancel();

        for (QuoteRequest q : new QuoteRequest[] {accepted, declined, cancelled}) {
            assertRefused(() -> q.quote(BigDecimal.ONE, null, NOW));
            assertRefused(() -> q.decline(null, NOW));
            assertRefused(q::cancel);
            assertRefused(q::accept);
        }
    }
}
