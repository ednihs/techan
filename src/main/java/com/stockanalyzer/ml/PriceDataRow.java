package com.stockanalyzer.ml;

import java.time.LocalDate;

public class PriceDataRow {
    private final LocalDate tradeDate;
    private final String symbol;
    private final long volume;
    private final int noOfTrades;

    public PriceDataRow(LocalDate tradeDate, String symbol, long volume, int noOfTrades) {
        this.tradeDate = tradeDate;
        this.symbol = symbol;
        this.volume = volume;
        this.noOfTrades = noOfTrades;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public String getSymbol() {
        return symbol;
    }

    public long getVolume() {
        return volume;
    }

    public int getNoOfTrades() {
        return noOfTrades;
    }
}
