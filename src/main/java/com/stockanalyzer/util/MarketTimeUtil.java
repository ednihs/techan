package com.stockanalyzer.util;

import java.time.LocalTime;

public class MarketTimeUtil {

    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 15);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(15, 30);

    public static boolean isMarketOpen(LocalTime time) {
        return !time.isBefore(MARKET_OPEN) && time.isBefore(MARKET_CLOSE);
    }
}
