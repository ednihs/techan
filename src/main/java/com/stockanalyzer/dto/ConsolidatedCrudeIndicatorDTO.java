package com.stockanalyzer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Consolidated DTO for crude oil OHLCV data and technical indicators.
 * Used for CSV/JSON export.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsolidatedCrudeIndicatorDTO {

    // OHLCV Data
    @CsvBindByName(column = "timestamp")
    @CsvDate("yyyy-MM-dd'T'HH:mm:ss'Z'")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime timestamp;

    @CsvBindByName(column = "timeframe")
    private String timeframe;

    @CsvBindByName(column = "open")
    private BigDecimal open;

    @CsvBindByName(column = "high")
    private BigDecimal high;

    @CsvBindByName(column = "low")
    private BigDecimal low;

    @CsvBindByName(column = "close")
    private BigDecimal close;

    @CsvBindByName(column = "volume")
    private Long volume;

    // Price Indicators
    @CsvBindByName(column = "rsi_14")
    private BigDecimal rsi14;

    @CsvBindByName(column = "macd_line")
    private BigDecimal macdLine;

    @CsvBindByName(column = "macd_signal")
    private BigDecimal macdSignal;

    @CsvBindByName(column = "macd_histogram")
    private BigDecimal macdHistogram;

    @CsvBindByName(column = "macd_crossover_signal")
    private String macdCrossoverSignal;

    @CsvBindByName(column = "bb_upper")
    private BigDecimal bbUpper;

    @CsvBindByName(column = "bb_middle")
    private BigDecimal bbMiddle;

    @CsvBindByName(column = "bb_lower")
    private BigDecimal bbLower;

    @CsvBindByName(column = "bb_width")
    private BigDecimal bbWidth;

    @CsvBindByName(column = "atr_14")
    private BigDecimal atr14;

    @CsvBindByName(column = "sma_20")
    private BigDecimal sma20;

    @CsvBindByName(column = "sma_50")
    private BigDecimal sma50;

    @CsvBindByName(column = "sma_100")
    private BigDecimal sma100;

    @CsvBindByName(column = "sma_200")
    private BigDecimal sma200;

    @CsvBindByName(column = "price_vs_sma20")
    private String priceVsSma20;

    @CsvBindByName(column = "price_vs_sma50")
    private String priceVsSma50;

    @CsvBindByName(column = "price_vs_sma100")
    private String priceVsSma100;

    @CsvBindByName(column = "price_vs_sma200")
    private String priceVsSma200;

    @CsvBindByName(column = "highest_20")
    private BigDecimal highest20;

    @CsvBindByName(column = "lowest_20")
    private BigDecimal lowest20;

    @CsvBindByName(column = "support_1")
    private BigDecimal support1;

    @CsvBindByName(column = "support_2")
    private BigDecimal support2;

    @CsvBindByName(column = "resistance_1")
    private BigDecimal resistance1;

    @CsvBindByName(column = "resistance_2")
    private BigDecimal resistance2;

    // Volume Indicators
    @CsvBindByName(column = "obv")
    private Long obv;

    @CsvBindByName(column = "obv_ema")
    private BigDecimal obvEma;

    @CsvBindByName(column = "vwap")
    private BigDecimal vwap;

    @CsvBindByName(column = "volume_sma_20")
    private Long volumeSma20;

    @CsvBindByName(column = "volume_ratio")
    private BigDecimal volumeRatio;

    @CsvBindByName(column = "price_volume_trend")
    private BigDecimal priceVolumeTrend;

    @CsvBindByName(column = "volume_rate_of_change")
    private BigDecimal volumeRateOfChange;

    // Metadata
    @CsvBindByName(column = "data_quality_flag")
    private String dataQualityFlag;

    @CsvBindByName(column = "day_type")
    private String dayType;
}

