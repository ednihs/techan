package com.stockanalyzer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for latest crude oil indicator summary.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LatestCrudeIndicatorDTO {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime timestamp;

    private BigDecimal close;
    private Long volume;  // Added for chart display
    private BigDecimal rsi14;
    private String macdCrossoverSignal;
    private Long obv;
    private BigDecimal vwap;
    private BigDecimal volumeRatio;
    private String dataQualityFlag;
}

