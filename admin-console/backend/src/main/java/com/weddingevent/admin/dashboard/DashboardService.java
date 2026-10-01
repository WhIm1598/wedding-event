package com.weddingevent.admin.dashboard;

import com.weddingevent.admin.asset.AssetService;
import com.weddingevent.admin.support.DateRange;
import com.weddingevent.common.domain.Asset;
import com.weddingevent.common.domain.AssetReservation;
import com.weddingevent.common.domain.enums.AssetCategory;
import com.weddingevent.common.domain.enums.AssetStatus;
import com.weddingevent.common.domain.enums.ContractStatus;
import com.weddingevent.common.domain.enums.LeadStage;
import com.weddingevent.common.domain.enums.ReservationStatus;
import com.weddingevent.common.domain.enums.TransactionType;
import com.weddingevent.common.repository.AssetRepository;
import com.weddingevent.common.repository.AssetReservationRepository;
import com.weddingevent.common.repository.ContractRepository;
import com.weddingevent.common.repository.LeadRepository;
import com.weddingevent.common.repository.PaymentTransactionRepository;
import com.weddingevent.common.repository.PaymentTransactionRepository.MonthlyTotal;
import com.weddingevent.common.util.VndFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Executive KPIs (FN-ADM-DASH-01). Trends compare this calendar month with the previous one. */
@Service
public class DashboardService {

    public record MoneyMetric(BigDecimal amount, String formatted, double trendPercentage, boolean isIncrease) {}

    public record CountMetric(long value, double trendPercentage, boolean isIncrease) {}

    public record RateMetric(double percentage, double trendPercentage, boolean isIncrease) {}

    public record StatsDto(MoneyMetric monthlyRevenue, CountMetric activeContractsCount, CountMetric pendingLeadsCount, RateMetric costumeRentalRate) {}

    public record RevenuePoint(int month, BigDecimal amount) {}

    private static final EnumSet<ContractStatus> ACTIVE_CONTRACTS = EnumSet.of(ContractStatus.DEPOSITED, ContractStatus.IN_PROGRESS);
    private static final EnumSet<LeadStage> PENDING_LEADS = EnumSet.of(LeadStage.NEW_LEAD, LeadStage.IN_CONSULTATION);
    private static final EnumSet<AssetCategory> COSTUMES = EnumSet.of(AssetCategory.DRESS, AssetCategory.SUIT);

    private final PaymentTransactionRepository transactions;
    private final ContractRepository contracts;
    private final LeadRepository leads;
    private final AssetRepository assets;
    private final AssetReservationRepository reservations;
    private final Clock clock;

    public DashboardService(PaymentTransactionRepository transactions, ContractRepository contracts, LeadRepository leads,
            AssetRepository assets, AssetReservationRepository reservations, Clock clock) {
        this.transactions = transactions;
        this.contracts = contracts;
        this.leads = leads;
        this.assets = assets;
        this.reservations = reservations;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public StatsDto stats() {
        LocalDate today = LocalDate.now(clock);
        DateRange thisMonth = DateRange.month(today);
        DateRange lastMonth = DateRange.month(YearMonth.from(today).minusMonths(1));

        BigDecimal revenue = transactions.sumByTypeBetween(TransactionType.INCOME, thisMonth.from(), thisMonth.to());
        BigDecimal revenueBefore = transactions.sumByTypeBetween(TransactionType.INCOME, lastMonth.from(), lastMonth.to());
        double revenueTrend = trend(revenue.doubleValue(), revenueBefore.doubleValue());

        // Active contracts: current count; trend = contracts signed this month vs last month
        long active = contracts.countByStatusIn(ACTIVE_CONTRACTS);
        double contractTrend = trend(contracts.countByContractDateBetween(thisMonth.from(), thisMonth.to()),
                contracts.countByContractDateBetween(lastMonth.from(), lastMonth.to()));

        // Pending leads: current count; trend = new pending leads this month vs last month
        var zone = clock.getZone();
        long pending = leads.countByStageIn(PENDING_LEADS);
        double leadTrend = trend(
                leads.countByStageInAndCreatedAtBetween(PENDING_LEADS, thisMonth.startInstant(zone), thisMonth.endInstantExclusive(zone)),
                leads.countByStageInAndCreatedAtBetween(PENDING_LEADS, lastMonth.startInstant(zone), lastMonth.endInstantExclusive(zone)));

        double rentalRate = costumeRentalRate(today);
        double rentalRateLastMonth = costumeRentalRate(today.minusMonths(1));

        return new StatsDto(
                new MoneyMetric(revenue, VndFormatter.vnd(revenue), revenueTrend, revenueTrend >= 0),
                new CountMetric(active, contractTrend, contractTrend >= 0),
                new CountMetric(pending, leadTrend, leadTrend >= 0),
                new RateMetric(rentalRate, round1(rentalRate - rentalRateLastMonth), rentalRate >= rentalRateLastMonth));
    }

    /** Monthly INCOME totals for the year, always 12 points */
    @Transactional(readOnly = true)
    public List<RevenuePoint> revenueChart(int year) {
        LocalDate from = LocalDate.of(year, 1, 1);
        Map<Integer, BigDecimal> byMonth = transactions.monthlyTotals(TransactionType.INCOME, from, from.withMonth(12).withDayOfMonth(31))
                .stream()
                .collect(Collectors.toMap(MonthlyTotal::getMonth, MonthlyTotal::getAmount));
        return IntStream.rangeClosed(1, 12).mapToObj(m -> new RevenuePoint(m, byMonth.getOrDefault(m, BigDecimal.ZERO))).toList();
    }

    /** % of dresses & suits rented (IN_USE) on the given day — spec: IN_USE / total costumes * 100 */
    private double costumeRentalRate(LocalDate day) {
        List<Asset> costumes = assets.findAllByOrderByCodeAsc().stream().filter(a -> COSTUMES.contains(a.getCategory())).toList();
        if (costumes.isEmpty()) {
            return 0;
        }
        Map<UUID, List<AssetReservation>> active = reservations
                .findByStatusAndLockEndDateGreaterThanEqualOrderByStartDateAsc(ReservationStatus.CONFIRMED, day).stream()
                .collect(Collectors.groupingBy(r -> r.getAsset().getId()));
        long inUse = costumes.stream()
                .filter(a -> AssetService.effectiveStatus(a, active.getOrDefault(a.getId(), List.of()), day) == AssetStatus.IN_USE)
                .count();
        return round1(inUse * 100.0 / costumes.size());
    }

    /** ((current - previous) / previous) * 100; 100% when growing from zero, 0% when both are zero */
    static double trend(double current, double previous) {
        if (previous == 0) {
            return current > 0 ? 100 : 0;
        }
        return round1((current - previous) / previous * 100);
    }

    private static double round1(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
