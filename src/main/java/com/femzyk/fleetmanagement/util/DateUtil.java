package com.femzyk.fleetmanagement.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Date helpers. All persistence uses ISO-8601 text so SQLite can compare and sort lexicographically. */
public final class DateUtil {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    public static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    public static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private DateUtil() {}

    public static String format(LocalDate date) {
        return date == null ? null : DATE.format(date);
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? null : DATE_TIME.format(dateTime);
    }

    public static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return LocalDate.parse(text.trim(), DATE);
    }

    public static LocalDateTime parseDateTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        try {
            return LocalDateTime.parse(t, DATE_TIME);
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(t);
        }
    }

    /** Lenient parser for user input: accepts yyyy-MM-dd, dd/MM/yyyy and dd-MM-yyyy. */
    public static LocalDate parseUserDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        DateTimeFormatter[] formats = {
            DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy")
        };
        for (DateTimeFormatter f : formats) {
            try {
                return LocalDate.parse(t, f);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        throw new DateTimeParseException("Unrecognised date: " + text, text, 0);
    }

    public static LocalDateTime parseUserDateTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        DateTimeFormatter[] formats = {
            DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
        };
        for (DateTimeFormatter f : formats) {
            try {
                return LocalDateTime.parse(t, f);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        LocalDate d = parseUserDate(t);
        return d.atStartOfDay();
    }

    public static String display(LocalDate date) {
        return date == null ? "" : DISPLAY_DATE.format(date);
    }

    public static String display(LocalDateTime dateTime) {
        return dateTime == null ? "" : DISPLAY_DATE_TIME.format(dateTime);
    }

    public static String monthKey(LocalDate date) {
        return MONTH.format(date);
    }

    public static String monthKey(YearMonth month) {
        return MONTH.format(month);
    }
}
