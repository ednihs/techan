package com.stockanalyzer.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MarketFeedData {
    private String symbol;
    private BigDecimal lastTradedPrice;
    private BigDecimal changePercent;
    private long volume;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal previousClose;
}
