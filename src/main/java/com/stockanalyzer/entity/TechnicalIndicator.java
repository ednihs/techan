package com.stockanalyzer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "technical_indicators")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class TechnicalIndicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;

    private Double rsi14;
    private Double ema9;
    private Double ema21;
    private Double sma20;
    private Double atr14;
    private Double vwap;

    private Double volumeSma20;
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

    // GROUP A: Historical Price Context (52-Week Data)
    @Column(name = "week_52_high")
    private Float week52High;

    @Column(name = "week_52_low")
    private Float week52Low;

    @Column(name = "days_since_52w_high")
    private Integer daysSince52wHigh;

    // GROUP B: Intraday Momentum Preservation
    @Column(name = "intraday_fade_pct")
    private Float intradayFadePct;

    // GROUP C: Price Velocity (Momentum Acceleration/Deceleration)
    @Column(name = "close_velocity_5d")
    private Float closeVelocity5d;

    // GROUP D: Previous Day Indicators
    @Column(name = "prev_day_open")
    private Float prevDayOpen;

    @Column(name = "prev_day_high")
    private Float prevDayHigh;

    @Column(name = "prev_day_low")
    private Float prevDayLow;

    @Column(name = "prev_day_close")
    private Float prevDayClose;

    @Column(name = "rsi_14_prev_day")
    private Float rsi14PrevDay;

    @Column(name = "macd_histogram_prev_day")
    private Float macdHistogramPrevDay;

    // GROUP E: Pivot Point Levels (from Previous Day OHLC)
    @Column(name = "pivot_point")
    private Float pivotPoint;

    @Column(name = "resistance_1")
    private Float resistance1;

    @Column(name = "resistance_2")
    private Float resistance2;

    @Column(name = "support_1")
    private Float support1;

    @Column(name = "support_2")
    private Float support2;

    // GROUP G: Derived Metrics for Exhaustion Detection
    @Column(name = "price_position_stage", length = 20)
    private String pricePositionStage;

    @Column(name = "momentum_direction", length = 20)
    private String momentumDirection;

    @Column(name = "data_completeness", length = 20)
    private String dataCompleteness;

    @Column(name = "delivery_strength_5d_avg")
    private Float deliveryStrength5dAvg;

    @Column(name = "delivery_strength_trend", length = 20)
    private String deliveryStrengthTrend;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
