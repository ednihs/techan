package com.stockanalyzer.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class BTSTDetailedAnalysisDTO {
    private String symbol;
    private LocalDate analysisDate;
    private Boolean hadLateSurge;
    private Double gapPercentage;
    private Boolean showsAbsorption;
    private Double averageTradeSize;
    private Double retailIntensity;
    private Boolean vwapReclaimed;
    private Double cumulativeDelta;
    private Double pullbackDepth;
    private Boolean supplyExhaustion;
    private Double strengthScore;
    private String recommendation;
    private Double confidenceScore;
    private Double entryPrice;
    private Double targetPrice;
    private Double stopLoss;
    private String liquidityRiskLevel;
    private String liquidityRiskFactors;
    private String gapRiskLevel;
    private String gapRiskFactors;
    private Integer catalystScore;
    private String catalystDetails;
}
