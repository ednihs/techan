package com.stockanalyzer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BTSTRecommendationDTO {
    private String symbol;
    private String sector;
    private String recommendation;
    private Double confidenceScore;
    private Double entryPrice;
    private Double targetPrice;
    private Double stopLoss;
    private Double strengthScore;
    private String liquidityRiskLevel;
    private String gapRiskLevel;
}
