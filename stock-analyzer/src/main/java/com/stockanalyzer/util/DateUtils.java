package com.stockanalyzer.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String format(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    public static LocalDate parse(String dateString) {
        return LocalDate.parse(dateString, DATE_FORMATTER);
    }
}
