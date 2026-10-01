package com.weddingevent.common.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** VND money and Vietnamese date formatting shared by all backends. */
public final class VndFormatter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private VndFormatter() {}

    /** 25000000 -> "25.000.000đ" */
    public static String vnd(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        return format.format(amount == null ? BigDecimal.ZERO : amount) + "đ";
    }

    /** 2026-10-20 -> "20/10/2026" */
    public static String date(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }
}
