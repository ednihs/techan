package com.stockanalyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "technical_indicators")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalIndicator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String symbol;
    
    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;
    
    // Price-based indicators
    @Column(name = "rsi_14")
    private Double rsi14;
    @Column(name = "ema_9")
    private Double ema9;
    @Column(name = "ema_21")
    private Double ema21;
    @Column(name = "sma_20")
    private Double sma20;
    @Column(name = "atr_14")
    private Double atr14;
    private Double vwap;
    
    // Volume-based indicators
    @Column(name = "volume_sma_20")
    private Long volumeSma20;
    @Column(name = "volume_ratio")
    private Double volumeRatio;
    
    // Custom indicators for our strategy
    @Column(name = "price_strength")
    private Double priceStrength;
    @Column(name = "volume_strength")
    private Double volumeStrength;
    @Column(name = "delivery_strength")
    private Double deliveryStrength;

    // Momentum indicators
    private Double macd;
    @Column(name = "macd_Signal")
    private Double macdSignal;
    @Column(name = "macd_histogram")
    private Double macdHistogram;

    // Volatility indicators
    @Column(name = "bollinger_upper")
    private Double bollingerUpper;
    @Column(name = "bollinger_lower")
    private Double bollingerLower;
    @Column(name = "bollinger_width")
    private Double bollingerWidth;
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
