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
@Table(name = "btst_analysis")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BTSTAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String symbol;
    
    @Column(name = "analysis_date", nullable = false)
    private LocalDate analysisDate;
    
    // Day-1 BTST characteristics
    private Boolean hadLateSurge;
    private Double lateSessionVolumeRatio;
    private Double breakoutLevel;
    private Boolean hadCatalyst;
    private String catalystType;
    
    // Day-2 Weak hands indicators
    private Double gapPercentage;
    private Boolean showsAbsorption;
    private Double averageTradeSize;
    private Double retailIntensity;
    private Boolean vwapReclaimed;
    private Double cumulativeDelta;
    
    // Technical setup
    private Double pullbackDepth;
    private Boolean supplyExhaustion;
    private Double strengthScore;
    
    // Final recommendation
    private String recommendation; // BUY, HOLD, AVOID
    private Double confidenceScore;
    private Double entryPrice;
    private Double targetPrice;
    private Double stopLoss;

    // Risk metrics
    private Double riskRewardRatio;
    private Double positionSizePercentage;

    // Automated risk assessment
    @Column(name = "liquidity_risk_level")
    private String liquidityRiskLevel;
    @Column(name = "liquidity_risk_factors")
    private String liquidityRiskFactors;
    @Column(name = "gap_risk_level")
    private String gapRiskLevel;
    @Column(name = "gap_risk_factors")
    private String gapRiskFactors;
    @Column(name = "catalyst_score")
    private Integer catalystScore;
    @Column(name = "catalyst_details")
    private String catalystDetails;
    @Column(name = "automated_risk_assessment")
    private Boolean automatedRiskAssessment;
    @Column(name = "position_size_percent")
    private Double positionSizePercent;
    @Column(name = "risk_reward_ratio_t1")
    private Double riskRewardRatioT1;
    @Column(name = "risk_reward_ratio_t2")
    private Double riskRewardRatioT2;

    @CreatedDate
    private LocalDateTime createdAt;
}
