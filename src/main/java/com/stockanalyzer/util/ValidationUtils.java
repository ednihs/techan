package com.stockanalyzer.util;

import java.util.Collection;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static boolean hasEnoughData(Collection<?> collection, int minimum) {
        return collection != null && collection.size() >= minimum;
    }
}
