package com.stockanalyzer.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class DateUtils {

    private static final LocalDate EPOCH = LocalDate.of(1970, 1, 1);

    private DateUtils() {
    }

    public static LocalDate resolveDateOrDefault(LocalDate candidate, LocalDate fallback) {
        if (candidate == null || EPOCH.equals(candidate)) {
            return fallback;
        }
        return candidate;
    }

    public static LocalDate getPreviousTradingDay(LocalDate date) {
        LocalDate previousDay = date.minusDays(1);
        if (previousDay.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return previousDay.minusDays(2);
        } else if (previousDay.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return previousDay.minusDays(1);
        }
        return previousDay;
    }
}
