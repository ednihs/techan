package com.stockanalyzer.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class BTSTRecommendationDTO {
    private String symbol;
    private String recommendation;
    private Double confidenceScore;
    private Double entryPrice;
    private Double targetPrice;
    private Double stopLoss;
    private Double strengthScore;
    private LocalDate analysisDate;
}
