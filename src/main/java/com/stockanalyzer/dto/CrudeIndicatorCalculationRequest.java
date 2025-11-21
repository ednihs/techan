package com.stockanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for crude oil indicator calculation API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrudeIndicatorCalculationRequest {

    /**
     * Symbol for crude oil (default: "BRN" for Brent Crude)
     */
    @Builder.Default
    private String symbol = "BRN";

    /**
     * Timeframes to process (e.g., ["4H", "1H", "15M"])
     */
    private List<String> timeframes;

    /**
     * Start timestamp for data processing (optional)
     */
    private LocalDateTime startDate;

    /**
     * End timestamp for data processing (optional)
     */
    private LocalDateTime endDate;

    /**
     * Whether to recalculate existing indicators (true) or skip them (false)
     */
    @Builder.Default
    private boolean recalculate = false;
}

