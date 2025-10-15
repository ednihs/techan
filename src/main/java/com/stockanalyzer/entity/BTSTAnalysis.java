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
@Table(name = "btst_analysis")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class BTSTAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(name = "analysis_date", nullable = false)
    private LocalDate analysisDate;

    // Day-1 BTST characteristics
    private Boolean hadLateSurge;
    private Double lateSessionVolumeRatio;
    private Double breakoutLevel;
    private Boolean hadCatalyst;
    private String catalystType;

    // Day-2 weak hands indicators
    private Double gapPercentage;
    private Boolean showsAbsorption;
    private Double averageTradeSize;
    private Double retailIntensity;
    private Boolean vwapReclaimed;
    private Double cumulativeDelta;

    // Technical setup metrics
    private Double pullbackDepth;
    private Boolean supplyExhaustion;
    private Double strengthScore;

    // Final recommendation
    private String recommendation;
    private Double confidenceScore;
    private Double entryPrice;
    private Double targetPrice;
    private Double stopLoss;

    // Automated risk fields
    @Column(name = "liquidity_risk_level")
    private String liquidityRiskLevel;
    @Column(name = "liquidity_risk_factors", columnDefinition = "TEXT")
    private String liquidityRiskFactors;
    @Column(name = "gap_risk_level")
    private String gapRiskLevel;
    @Column(name = "gap_risk_factors", columnDefinition = "TEXT")
    private String gapRiskFactors;
    private Integer catalystScore;
    @Column(name = "catalyst_details", columnDefinition = "TEXT")
    private String catalystDetails;
    private Boolean automatedRiskAssessment;
    private Double positionSizePercent;
    private Double riskRewardRatioT1;
    private Double riskRewardRatioT2;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
