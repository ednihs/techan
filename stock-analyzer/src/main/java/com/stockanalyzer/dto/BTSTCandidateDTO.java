package com.stockanalyzer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BTSTCandidateDTO {
    private String symbol;
    private Double volumeRatio;
    // Other fields will be added here
}
