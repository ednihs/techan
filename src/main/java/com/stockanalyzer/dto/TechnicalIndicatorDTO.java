package com.stockanalyzer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TechnicalIndicatorDTO {
    private String symbol;
    private Double rsi14;
    private Double ema9;
    private Double ema21;
    private Double sma20;
    private Double atr14;
    private Double vwap;
    private Double volumeRatio;
    private Double priceStrength;
    private Double volumeStrength;
    private Double deliveryStrength;
}
