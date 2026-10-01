package com.weddingevent.admin.support;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.Instant;
import java.time.temporal.TemporalAdjusters;

/** Inclusive date range helpers for calendar views and month-over-month KPIs. */
public record DateRange(LocalDate from, LocalDate to) {

    public static DateRange day(LocalDate date) {
        return new DateRange(date, date);
    }

    /** Monday-based week containing {@code date} */
    public static DateRange week(LocalDate date) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new DateRange(monday, monday.plusDays(6));
    }

    public static DateRange month(YearMonth month) {
        return new DateRange(month.atDay(1), month.atEndOfMonth());
    }

    public static DateRange month(LocalDate date) {
        return month(YearMonth.from(date));
    }

    public Instant startInstant(ZoneId zone) {
        return from.atStartOfDay(zone).toInstant();
    }

    public Instant endInstantExclusive(ZoneId zone) {
        return to.plusDays(1).atStartOfDay(zone).toInstant();
    }
}
