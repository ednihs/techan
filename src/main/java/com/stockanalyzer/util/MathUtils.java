package com.stockanalyzer.util;

public final class MathUtils {

    private MathUtils() {
    }

    public static double safeDivide(double numerator, double denominator) {
        if (denominator == 0.0d) {
            return 0.0d;
        }
        return numerator / denominator;
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
