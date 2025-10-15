package com.stockanalyzer.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class BTSTCandidateDTO {
    private String symbol;
    private LocalDate tradeDate;
    private Double closePrice;
    private Double breakoutLevel;
    private Double volumeRatio;
    private Double strengthScore;
    private boolean breakoutConfirmed;
}
