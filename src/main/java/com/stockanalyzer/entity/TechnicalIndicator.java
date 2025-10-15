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

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
