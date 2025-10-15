package com.stockanalyzer.util;

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
}
