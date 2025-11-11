package com.stockanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalIndicatorDTO {
    private String symbol;
    private LocalDate calculationDate;
    private LocalDateTime createdAt;
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
    private Double macd;
    private Double macdSignal;
    private Double macdHistogram;
    private Double bollingerUpper;
    private Double bollingerLower;
    private Double bollingerWidth;
}
