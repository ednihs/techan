package com.stockanalyzer.dto;

import com.opencsv.bean.CsvBindByName;
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
    @CsvBindByName(column = "Symbol")
    private String symbol;
    @CsvBindByName(column = "Calculation Date")
    private LocalDate calculationDate;
    @CsvBindByName(column = "Created At")
    private LocalDateTime createdAt;
    @CsvBindByName(column = "RSI 14")
    private Double rsi14;
    @CsvBindByName(column = "EMA 9")
    private Double ema9;
    @CsvBindByName(column = "EMA 21")
    private Double ema21;
    @CsvBindByName(column = "SMA 20")
    private Double sma20;
    @CsvBindByName(column = "ATR 14")
    private Double atr14;
    @CsvBindByName(column = "VWAP")
    private Double vwap;
    @CsvBindByName(column = "Volume Ratio")
    private Double volumeRatio;
    @CsvBindByName(column = "Price Strength")
    private Double priceStrength;
    @CsvBindByName(column = "Volume Strength")
    private Double volumeStrength;
    @CsvBindByName(column = "Delivery Strength")
    private Double deliveryStrength;
    @CsvBindByName(column = "MACD")
    private Double macd;
    @CsvBindByName(column = "MACD Signal")
    private Double macdSignal;
    @CsvBindByName(column = "MACD Histogram")
    private Double macdHistogram;
    @CsvBindByName(column = "Bollinger Upper")
    private Double bollingerUpper;
    @CsvBindByName(column = "Bollinger Lower")
    private Double bollingerLower;
    @CsvBindByName(column = "Bollinger Width")
    private Double bollingerWidth;

    // GROUP A: Historical Price Context (52-Week Data)
    @CsvBindByName(column = "52 Week High")
    private Float week52High;
    @CsvBindByName(column = "52 Week Low")
    private Float week52Low;
    @CsvBindByName(column = "Days Since 52W High")
    private Integer daysSince52wHigh;

    // GROUP B: Intraday Momentum Preservation
    @CsvBindByName(column = "Intraday Fade Pct")
    private Float intradayFadePct;

    // GROUP C: Price Velocity (Momentum Acceleration/Deceleration)
    @CsvBindByName(column = "Close Velocity 5D")
    private Float closeVelocity5d;

    // GROUP D: Previous Day Indicators
    @CsvBindByName(column = "Prev Day Open")
    private Float prevDayOpen;
    @CsvBindByName(column = "Prev Day High")
    private Float prevDayHigh;
    @CsvBindByName(column = "Prev Day Low")
    private Float prevDayLow;
    @CsvBindByName(column = "Prev Day Close")
    private Float prevDayClose;
    @CsvBindByName(column = "RSI 14 Prev Day")
    private Float rsi14PrevDay;
    @CsvBindByName(column = "MACD Histogram Prev Day")
    private Float macdHistogramPrevDay;

    // GROUP E: Pivot Point Levels (from Previous Day OHLC)
    @CsvBindByName(column = "Pivot Point")
    private Float pivotPoint;
    @CsvBindByName(column = "Resistance 1")
    private Float resistance1;
    @CsvBindByName(column = "Resistance 2")
    private Float resistance2;
    @CsvBindByName(column = "Support 1")
    private Float support1;
    @CsvBindByName(column = "Support 2")
    private Float support2;

    // GROUP F: Distance Percentages to Pivot Levels (Dynamically Calculated)
    @CsvBindByName(column = "Pct From Pivot")
    private Float pctFromPivot;
    @CsvBindByName(column = "Pct to Resistance 1")
    private Float pctToResistance1;
    @CsvBindByName(column = "Pct to Resistance 2")
    private Float pctToResistance2;
    @CsvBindByName(column = "Pct to Support 1")
    private Float pctToSupport1;
    @CsvBindByName(column = "Pct to Support 2")
    private Float pctToSupport2;

    // GROUP G: Derived Metrics for Exhaustion Detection
    @CsvBindByName(column = "Price Position Stage")
    private String pricePositionStage;
    @CsvBindByName(column = "Momentum Direction")
    private String momentumDirection;
    @CsvBindByName(column = "Data Completeness")
    private String dataCompleteness;
    
    @CsvBindByName(column = "Delivery Strength 5D Avg")
    private Float deliveryStrength5dAvg;
    
    @CsvBindByName(column = "Delivery Strength Trend")
    private String deliveryStrengthTrend;
}
