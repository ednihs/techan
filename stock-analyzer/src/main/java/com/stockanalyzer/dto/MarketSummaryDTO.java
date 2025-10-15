package com.stockanalyzer.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class MarketSummaryDTO {
    private LocalDate analysisDate;
    private int totalCandidates;
    private int buyRecommendations;
    private int holdRecommendations;
    private int avoidRecommendations;
    private double avgConfidenceScore;
}
