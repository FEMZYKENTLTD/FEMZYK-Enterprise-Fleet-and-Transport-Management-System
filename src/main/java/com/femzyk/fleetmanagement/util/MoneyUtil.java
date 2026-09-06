package com.femzyk.fleetmanagement.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/** Currency formatting. Amounts are stored as REAL in SQLite and rounded to 2 dp for display. */
public final class MoneyUtil {

    public static final String CURRENCY_SYMBOL = "\u20A6"; // Nigerian Naira
    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.00");

    private MoneyUtil() {}

    public static String format(double amount) {
        return CURRENCY_SYMBOL + FORMAT.format(amount);
    }

    public static String formatPlain(double amount) {
        return FORMAT.format(amount);
    }

    public static double round(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
