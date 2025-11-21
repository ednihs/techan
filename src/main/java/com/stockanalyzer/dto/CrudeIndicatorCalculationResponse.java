package com.stockanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for crude oil indicator calculation API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrudeIndicatorCalculationResponse {

    private boolean success;
    private String message;
    private CalculationData data;
    private String error;
    private String details;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CalculationData {
        /**
         * Number of records processed per timeframe
         */
        private Map<String, Integer> processed;

        /**
         * Total records processed across all timeframes
         */
        private int totalRecords;

        /**
         * Execution time in milliseconds
         */
        private long executionTimeMs;
    }

    public static CrudeIndicatorCalculationResponse success(String message, CalculationData data) {
        return CrudeIndicatorCalculationResponse.builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static CrudeIndicatorCalculationResponse error(String error, String details) {
        return CrudeIndicatorCalculationResponse.builder()
                .success(false)
                .error(error)
                .details(details)
                .build();
    }
}

