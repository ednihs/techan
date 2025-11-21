package com.stockanalyzer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing calculated technical indicators for crude oil.
 * Includes both price-based and volume-based indicators.
 */
@Entity
@Table(name = "crude_technical_indicators",
        uniqueConstraints = @UniqueConstraint(name = "unique_indicator", columnNames = {"ohlcv_id"}),
        indexes = {
                @Index(name = "idx_timeframe_timestamp", columnList = "timeframe, timestamp DESC"),
                @Index(name = "idx_data_quality", columnList = "dataQualityFlag, timestamp DESC")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CrudeTechnicalIndicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ohlcv_id", nullable = false)
    private Long ohlcvId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CrudeOHLCVData.Timeframe timeframe;

    // Price-based indicators
    @Column(name = "rsi_14", precision = 6, scale = 2)
    private BigDecimal rsi14;

    @Column(name = "macd_line", precision = 8, scale = 4)
    private BigDecimal macdLine;

    @Column(name = "macd_signal", precision = 8, scale = 4)
    private BigDecimal macdSignal;

    @Column(name = "macd_histogram", precision = 8, scale = 4)
    private BigDecimal macdHistogram;

    @Enumerated(EnumType.STRING)
    @Column(name = "macd_crossover_signal", length = 8)
    private MacdSignal macdCrossoverSignal;

    @Column(name = "bb_upper", precision = 10, scale = 4)
    private BigDecimal bbUpper;

    @Column(name = "bb_middle", precision = 10, scale = 4)
    private BigDecimal bbMiddle;

    @Column(name = "bb_lower", precision = 10, scale = 4)
    private BigDecimal bbLower;

    @Column(name = "bb_width", precision = 8, scale = 4)
    private BigDecimal bbWidth;

    @Column(name = "atr_14", precision = 8, scale = 4)
    private BigDecimal atr14;

    @Column(name = "sma_20", precision = 10, scale = 4)
    private BigDecimal sma20;

    @Column(name = "sma_50", precision = 10, scale = 4)
    private BigDecimal sma50;

    @Column(name = "sma_100", precision = 10, scale = 4)
    private BigDecimal sma100;

    @Column(name = "sma_200", precision = 10, scale = 4)
    private BigDecimal sma200;

    @Column(name = "ema_12", precision = 10, scale = 4)
    private BigDecimal ema12;

    @Column(name = "ema_26", precision = 10, scale = 4)
    private BigDecimal ema26;

    // Price vs SMA percentages
    @Column(name = "price_vs_sma20", length = 10)
    private String priceVsSma20;

    @Column(name = "price_vs_sma50", length = 10)
    private String priceVsSma50;

    @Column(name = "price_vs_sma100", length = 10)
    private String priceVsSma100;

    @Column(name = "price_vs_sma200", length = 10)
    private String priceVsSma200;

    // Support/Resistance levels
    @Column(name = "highest_20", precision = 10, scale = 4)
    private BigDecimal highest20;

    @Column(name = "lowest_20", precision = 10, scale = 4)
    private BigDecimal lowest20;

    @Column(name = "support_1", precision = 10, scale = 4)
    private BigDecimal support1;

    @Column(name = "support_2", precision = 10, scale = 4)
    private BigDecimal support2;

    @Column(name = "resistance_1", precision = 10, scale = 4)
    private BigDecimal resistance1;

    @Column(name = "resistance_2", precision = 10, scale = 4)
    private BigDecimal resistance2;

    // Volume-based indicators
    @Column(name = "obv")
    private Long obv; // On-Balance Volume

    @Column(name = "obv_ema", precision = 12, scale = 2)
    private BigDecimal obvEma;

    @Column(name = "vwap", precision = 10, scale = 4)
    private BigDecimal vwap;

    @Column(name = "volume_sma_20")
    private Long volumeSma20;

    @Column(name = "volume_ratio", precision = 6, scale = 2)
    private BigDecimal volumeRatio;

    @Column(name = "price_volume_trend", precision = 12, scale = 2)
    private BigDecimal priceVolumeTrend;

    @Column(name = "volume_rate_of_change", precision = 8, scale = 2)
    private BigDecimal volumeRateOfChange;

    // Metadata
    @Enumerated(EnumType.STRING)
    @Column(name = "data_quality_flag", nullable = false, length = 6)
    private DataQualityFlag dataQualityFlag;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", length = 12)
    private DayType dayType;

    @CreatedDate
    @Column(name = "calculated_at", updatable = false)
    private LocalDateTime calculatedAt;

    public enum MacdSignal {
        bullish, bearish, neutral
    }

    public enum DataQualityFlag {
        Fresh,   // Data within last hour
        Recent,  // Data within last 24 hours
        Stale    // Data older than 24 hours
    }

    public enum DayType {
        Regular,
        EIA_Release,
        Fed_Event,
        OPEC_Update,
        Geopolitical
    }
}

