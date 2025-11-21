package com.stockanalyzer.controller;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.stockanalyzer.dto.*;
import com.stockanalyzer.service.CrudeOilIndicatorService;
import com.stockanalyzer.service.CrudeOHLCVDataLoaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for crude oil technical indicator operations.
 * Provides endpoints for calculating indicators, downloading consolidated data, and real-time queries.
 */
@RestController
@RequestMapping("/api/v1/crude")
@RequiredArgsConstructor
@Slf4j
public class CrudeOilController {

    private final CrudeOilIndicatorService crudeOilIndicatorService;
    private final CrudeOHLCVDataLoaderService ohlcvDataLoaderService;

    /**
     * API 1: Trigger Indicator Calculation
     * POST /api/v1/crude/calculate-indicators
     * 
     * Fetches OHLCV data, calculates all technical indicators (price + volume),
     * and saves to crude_technical_indicators table.
     */
    @PostMapping("/calculate-indicators")
    public ResponseEntity<CrudeIndicatorCalculationResponse> calculateIndicators(
            @RequestBody CrudeIndicatorCalculationRequest request) {
        
        log.info("POST /api/v1/crude/calculate-indicators called with request: {}", request);

        try {
            // Validate request
            if (request.getSymbol() == null || request.getSymbol().isEmpty()) {
                request.setSymbol("BRN");
            }

            CrudeIndicatorCalculationResponse response = crudeOilIndicatorService.calculateIndicators(request);
            
            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            log.error("Error in calculate-indicators endpoint", e);
            CrudeIndicatorCalculationResponse errorResponse = CrudeIndicatorCalculationResponse.error(
                    "Internal server error",
                    e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * API 2: Download Consolidated Indicator Data
     * GET /api/v1/crude/download-indicators
     * 
     * Exports consolidated technical indicator data as CSV or JSON file.
     * All timeframes can be included in a single file.
     */
    @GetMapping("/download-indicators")
    public void downloadIndicators(
            @RequestParam(defaultValue = "4H,1H,15M") String timeframes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String dataQuality,
            @RequestParam(defaultValue = "csv") String format,
            HttpServletResponse response) throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {

        log.info("GET /api/v1/crude/download-indicators called with timeframes: {}, startDate: {}, endDate: {}, format: {}",
                timeframes, startDate, endDate, format);

        try {
            // Parse parameters
            List<String> timeframeList = Arrays.asList(timeframes.split(","));
            List<String> dataQualityList = dataQuality != null ? Arrays.asList(dataQuality.split(",")) : null;

            // Fetch consolidated data
            List<ConsolidatedCrudeIndicatorDTO> data = crudeOilIndicatorService.getConsolidatedData(
                    timeframeList, startDate, endDate, dataQualityList
            );

            if ("json".equalsIgnoreCase(format)) {
                exportAsJson(data, startDate, endDate, response);
            } else {
                exportAsCsv(data, startDate, endDate, response);
            }

        } catch (Exception e) {
            log.error("Error downloading crude oil indicators", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error: " + e.getMessage());
        }
    }

    /**
     * Export data as CSV
     */
    private void exportAsCsv(List<ConsolidatedCrudeIndicatorDTO> data,
                            LocalDateTime startDate, LocalDateTime endDate,
                            HttpServletResponse response) throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
        
        String filename = generateFilename("csv", startDate, endDate);
        
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        StatefulBeanToCsv<ConsolidatedCrudeIndicatorDTO> writer = new StatefulBeanToCsvBuilder<ConsolidatedCrudeIndicatorDTO>(response.getWriter())
                .withQuotechar('"')
                .withSeparator(',')
                .withOrderedResults(true)
                .build();

        writer.write(data);
        response.getWriter().flush();
    }

    /**
     * Export data as JSON
     */
    private void exportAsJson(List<ConsolidatedCrudeIndicatorDTO> data,
                             LocalDateTime startDate, LocalDateTime endDate,
                             HttpServletResponse response) throws IOException {
        
        String filename = generateFilename("json", startDate, endDate);
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        // Build JSON response with metadata
        Map<String, Object> jsonResponse = new HashMap<>();
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("total_records", data.size());
        metadata.put("timeframes", data.stream()
                .map(ConsolidatedCrudeIndicatorDTO::getTimeframe)
                .distinct()
                .collect(Collectors.toList()));
        
        Map<String, String> dateRange = new HashMap<>();
        dateRange.put("start", startDate != null ? startDate.format(DateTimeFormatter.ISO_DATE_TIME) : "N/A");
        dateRange.put("end", endDate != null ? endDate.format(DateTimeFormatter.ISO_DATE_TIME) : "N/A");
        metadata.put("date_range", dateRange);
        metadata.put("generated_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        
        jsonResponse.put("success", true);
        jsonResponse.put("metadata", metadata);
        jsonResponse.put("data", data);

        // Use Jackson ObjectMapper (assumes it's configured in your Spring Boot app)
        response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(jsonResponse));
        response.getWriter().flush();
    }

    /**
     * Generate filename for export
     */
    private String generateFilename(String format, LocalDateTime startDate, LocalDateTime endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");
        String dateStr = LocalDateTime.now().format(formatter);
        return String.format("CRUDE_OIL_CONSOLIDATED_%s_INDICATORS.%s", dateStr, format);
    }

    /**
     * API 3: Get Latest Indicators (Real-time Query)
     * GET /api/v1/crude/indicators/latest
     * 
     * Returns the most recent indicator values for quick analysis.
     */
    @GetMapping("/indicators/latest")
    public ResponseEntity<Map<String, Object>> getLatestIndicators(
            @RequestParam(defaultValue = "4H,1H,15M") String timeframes,
            @RequestParam(defaultValue = "20") int limit) {

        log.info("GET /api/v1/crude/indicators/latest called with timeframes: {}, limit: {}", timeframes, limit);

        try {
            List<String> timeframeList = Arrays.asList(timeframes.split(","));

            Map<String, List<LatestCrudeIndicatorDTO>> data = crudeOilIndicatorService.getLatestIndicators(
                    timeframeList, limit
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching latest crude oil indicators", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to fetch latest indicators");
            errorResponse.put("details", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Crude Oil Indicator Service");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        return ResponseEntity.ok(response);
    }

    /**
     * API 4: Load OHLCV Data
     * POST /api/v1/crude/load-ohlcv-data
     * 
     * Loads OHLCV data from FivePaisa service into crude_ohlcv_data table.
     * This is a helper endpoint to populate data before calculating indicators.
     */
    @PostMapping("/load-ohlcv-data")
    public ResponseEntity<Map<String, Object>> loadOHLCVData(
            @RequestParam(defaultValue = "BRN") String symbol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("POST /api/v1/crude/load-ohlcv-data called with symbol: {}, startDate: {}, endDate: {}", 
                symbol, startDate, endDate);

        try {
            // Set defaults
            if (endDate == null) endDate = LocalDate.now();
            if (startDate == null) startDate = endDate.minusDays(60);

            // Load data for all timeframes
            int recordsLoaded = ohlcvDataLoaderService.loadAllTimeframes(symbol, startDate, endDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "OHLCV data loaded successfully");
            response.put("recordsLoaded", recordsLoaded);
            response.put("symbol", symbol);
            response.put("startDate", startDate.toString());
            response.put("endDate", endDate.toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error loading OHLCV data", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to load OHLCV data");
            errorResponse.put("details", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get OHLCV data statistics
     * GET /api/v1/crude/ohlcv-stats
     * 
     * Returns statistics about available OHLCV data
     */
    @GetMapping("/ohlcv-stats")
    public ResponseEntity<Map<String, Object>> getOHLCVStats(
            @RequestParam(defaultValue = "BRN") String symbol) {

        log.info("GET /api/v1/crude/ohlcv-stats called with symbol: {}", symbol);

        try {
            Map<String, Object> response = new HashMap<>();
            Map<String, Long> counts = new HashMap<>();

            counts.put("4H", ohlcvDataLoaderService.getOHLCVCount(symbol, 
                    com.stockanalyzer.entity.CrudeOHLCVData.Timeframe.FOUR_H));
            counts.put("1H", ohlcvDataLoaderService.getOHLCVCount(symbol, 
                    com.stockanalyzer.entity.CrudeOHLCVData.Timeframe.ONE_H));
            counts.put("15M", ohlcvDataLoaderService.getOHLCVCount(symbol, 
                    com.stockanalyzer.entity.CrudeOHLCVData.Timeframe.FIFTEEN_M));

            response.put("success", true);
            response.put("symbol", symbol);
            response.put("counts", counts);
            response.put("ready_for_calculation", counts.values().stream().allMatch(c -> c >= 200));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting OHLCV stats", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to get OHLCV stats");
            errorResponse.put("details", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get API documentation
     */
    @GetMapping("/api-info")
    public ResponseEntity<Map<String, Object>> getApiInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "Crude Oil Technical Analysis API");
        info.put("version", "1.0");
        info.put("base_url", "/api/v1/crude");
        
        List<Map<String, String>> endpoints = Arrays.asList(
                createEndpointInfo("POST", "/load-ohlcv-data", "Load OHLCV data from data source"),
                createEndpointInfo("GET", "/ohlcv-stats", "Get OHLCV data statistics"),
                createEndpointInfo("POST", "/calculate-indicators", "Trigger indicator calculation for crude oil data"),
                createEndpointInfo("GET", "/download-indicators", "Download consolidated indicator data as CSV or JSON"),
                createEndpointInfo("GET", "/indicators/latest", "Get latest indicators for real-time analysis"),
                createEndpointInfo("GET", "/health", "Health check endpoint"),
                createEndpointInfo("GET", "/api-info", "API documentation")
        );
        
        info.put("endpoints", endpoints);
        
        return ResponseEntity.ok(info);
    }

    private Map<String, String> createEndpointInfo(String method, String path, String description) {
        Map<String, String> endpoint = new HashMap<>();
        endpoint.put("method", method);
        endpoint.put("path", path);
        endpoint.put("description", description);
        return endpoint;
    }
}

