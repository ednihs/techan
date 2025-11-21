package com.stockanalyzer.service;

import com.stockanalyzer.entity.CrudeOHLCVData;
import com.stockanalyzer.entity.PriceData;
import com.stockanalyzer.repository.CrudeOHLCVDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service to load OHLCV data into crude_ohlcv_data table from existing sources.
 * Bridges the existing getCrudeOilIndicators functionality with the new schema.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CrudeOHLCVDataLoaderService {

    private final FivePaisaService fivePaisaService;
    private final CrudeOHLCVDataRepository ohlcvRepository;

    /**
     * Load crude oil OHLCV data from FivePaisa for a specific timeframe and date range.
     * This method fetches historical data and stores it in the crude_ohlcv_data table.
     * 
     * @param symbol Symbol (default: "BRN" for Brent Crude)
     * @param interval Interval from FivePaisa (e.g., "15m", "60m"). Note: 5paisa doesn't support "1h" or "4h"
     * @param timeframe Timeframe enum for our system
     * @param startDate Start date
     * @param endDate End date
     * @return Number of records loaded
     */
    @Transactional
    public int loadOHLCVData(String symbol, String interval, CrudeOHLCVData.Timeframe timeframe, 
                            LocalDate startDate, LocalDate endDate) {
        log.info("Loading OHLCV data for symbol: {}, interval: {}, timeframe: {}, from {} to {}", 
                symbol, interval, timeframe, startDate, endDate);

        // Fetch data from FivePaisa using existing service
        // Note: You'll need to get the scripCode for crude oil from your system
        int scripCode = 462523; // CRUDEOIL scripCode
        List<PriceData> priceDataList = fivePaisaService.getHistoricalData(scripCode, interval, startDate, endDate);

        if (priceDataList == null || priceDataList.isEmpty()) {
            log.warn("No data fetched from FivePaisa for interval: {}", interval);
            return 0;
        }

        log.info("Fetched {} records from FivePaisa", priceDataList.size());

        // Convert PriceData to CrudeOHLCVData
        List<CrudeOHLCVData> crudeOHLCVList = priceDataList.stream()
                .map(priceData -> convertToCrudeOHLCV(priceData, symbol, timeframe))
                .collect(Collectors.toList());

        // Save to database (skip duplicates)
        int saved = 0;
        for (CrudeOHLCVData ohlcv : crudeOHLCVList) {
            if (!ohlcvRepository.existsBySymbolAndTimeframeAndTimestamp(
                    ohlcv.getSymbol(), ohlcv.getTimeframe(), ohlcv.getTimestamp())) {
                ohlcvRepository.save(ohlcv);
                saved++;
            }
        }

        log.info("Saved {} new OHLCV records (skipped {} duplicates)", saved, crudeOHLCVList.size() - saved);
        return saved;
    }

    /**
     * Load data for all timeframes
     */
    @Transactional
    public int loadAllTimeframes(String symbol, LocalDate startDate, LocalDate endDate) {
        int total = 0;

        // Load 15M data
        total += loadOHLCVData(symbol, "15m", CrudeOHLCVData.Timeframe.FIFTEEN_M, startDate, endDate);

        // Load 1H data (using 60m as 5paisa supports this format)
        total += loadOHLCVData(symbol, "60m", CrudeOHLCVData.Timeframe.ONE_H, startDate, endDate);

        // Load 4H data - computed from 60m data since 5paisa doesn't support 4h directly
        total += load4HDataFrom60m(symbol, startDate, endDate);

        return total;
    }

    /**
     * Load 4H candles by aggregating 60m candles
     * Since 5paisa doesn't support 4h interval, we fetch 60m data and aggregate it
     */
    @Transactional
    public int load4HDataFrom60m(String symbol, LocalDate startDate, LocalDate endDate) {
        log.info("Loading 4H data by aggregating 60m candles for symbol: {}, from {} to {}", 
                symbol, startDate, endDate);

        // Fetch 60m data from 5paisa
        int scripCode = 462523; // CRUDEOIL scripCode
        List<PriceData> priceDataList60m = fivePaisaService.getHistoricalData(scripCode, "60m", startDate, endDate);

        if (priceDataList60m == null || priceDataList60m.isEmpty()) {
            log.warn("No 60m data available to aggregate into 4H candles");
            return 0;
        }

        log.info("Fetched {} 60m records to aggregate into 4H candles", priceDataList60m.size());

        // Sort by timestamp
        List<PriceData> sortedData = priceDataList60m.stream()
                .sorted(Comparator.comparing(PriceData::getCreatedAt))
                .collect(Collectors.toList());

        // Aggregate into 4H candles
        List<CrudeOHLCVData> fourHourCandles = aggregate60mTo4H(sortedData, symbol);

        // Save to database (skip duplicates)
        int saved = 0;
        for (CrudeOHLCVData ohlcv : fourHourCandles) {
            if (!ohlcvRepository.existsBySymbolAndTimeframeAndTimestamp(
                    ohlcv.getSymbol(), ohlcv.getTimeframe(), ohlcv.getTimestamp())) {
                ohlcvRepository.save(ohlcv);
                saved++;
            }
        }

        log.info("Created {} 4H candles from 60m data (saved {} new, skipped {} duplicates)", 
                fourHourCandles.size(), saved, fourHourCandles.size() - saved);
        return saved;
    }

    /**
     * Aggregate 60-minute candles into 4-hour candles
     * Groups candles by 4-hour windows (00:00-04:00, 04:00-08:00, 08:00-12:00, etc.)
     */
    private List<CrudeOHLCVData> aggregate60mTo4H(List<PriceData> priceDataList, String symbol) {
        List<CrudeOHLCVData> fourHourCandles = new ArrayList<>();
        
        if (priceDataList.isEmpty()) {
            return fourHourCandles;
        }

        List<PriceData> currentWindow = new ArrayList<>();
        LocalDateTime currentWindowStart = null;

        for (PriceData priceData : priceDataList) {
            LocalDateTime timestamp = priceData.getCreatedAt();
            
            // Determine which 4-hour window this candle belongs to
            LocalDateTime windowStart = get4HourWindowStart(timestamp);
            
            // If we're starting a new window
            if (currentWindowStart == null || !windowStart.equals(currentWindowStart)) {
                // Save the previous window if it has data
                if (!currentWindow.isEmpty()) {
                    CrudeOHLCVData aggregated = aggregate4HCandle(currentWindow, symbol, currentWindowStart);
                    if (aggregated != null) {
                        fourHourCandles.add(aggregated);
                    }
                }
                
                // Start new window
                currentWindow = new ArrayList<>();
                currentWindowStart = windowStart;
            }
            
            // Add to current window
            currentWindow.add(priceData);
        }

        // Don't forget the last window
        if (!currentWindow.isEmpty() && currentWindowStart != null) {
            CrudeOHLCVData aggregated = aggregate4HCandle(currentWindow, symbol, currentWindowStart);
            if (aggregated != null) {
                fourHourCandles.add(aggregated);
            }
        }

        return fourHourCandles;
    }

    /**
     * Get the start of the 4-hour window for a given timestamp
     * Windows: 00:00, 04:00, 08:00, 12:00, 16:00, 20:00
     */
    private LocalDateTime get4HourWindowStart(LocalDateTime timestamp) {
        int hour = timestamp.getHour();
        int windowHour = (hour / 4) * 4; // Round down to nearest 4-hour mark
        return timestamp.toLocalDate().atTime(windowHour, 0, 0);
    }

    /**
     * Aggregate a list of 60m candles into a single 4H candle
     */
    private CrudeOHLCVData aggregate4HCandle(List<PriceData> candles, String symbol, LocalDateTime windowStart) {
        if (candles.isEmpty()) {
            return null;
        }

        // Sort by timestamp to ensure correct order
        List<PriceData> sorted = candles.stream()
                .sorted(Comparator.comparing(PriceData::getCreatedAt))
                .collect(Collectors.toList());

        // OHLCV aggregation:
        // Open: First candle's open
        // High: Maximum of all highs
        // Low: Minimum of all lows
        // Close: Last candle's close
        // Volume: Sum of all volumes

        BigDecimal open = sorted.get(0).getOpenPrice();
        BigDecimal close = sorted.get(sorted.size() - 1).getClosePrice();
        
        BigDecimal high = sorted.stream()
                .map(PriceData::getHighPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        BigDecimal low = sorted.stream()
                .map(PriceData::getLowPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        Long volume = sorted.stream()
                .map(PriceData::getVolume)
                .reduce(0L, Long::sum);

        return CrudeOHLCVData.builder()
                .timestamp(windowStart)
                .timeframe(CrudeOHLCVData.Timeframe.FOUR_H)
                .symbol(symbol)
                .open(open)
                .high(high)
                .low(low)
                .close(close)
                .volume(volume)
                .build();
    }

    /**
     * Convert PriceData to CrudeOHLCVData
     */
    private CrudeOHLCVData convertToCrudeOHLCV(PriceData priceData, String symbol, 
                                               CrudeOHLCVData.Timeframe timeframe) {
        // Use createdAt as timestamp (which includes time component from intraday data)
        LocalDateTime timestamp = priceData.getCreatedAt() != null 
                ? priceData.getCreatedAt() 
                : priceData.getTradeDate().atStartOfDay();

        return CrudeOHLCVData.builder()
                .timestamp(timestamp)
                .timeframe(timeframe)
                .symbol(symbol)
                .open(priceData.getOpenPrice())
                .high(priceData.getHighPrice())
                .low(priceData.getLowPrice())
                .close(priceData.getClosePrice())
                .volume(priceData.getVolume())
                .build();
    }

    /**
     * Get count of available OHLCV records
     */
    public long getOHLCVCount(String symbol, CrudeOHLCVData.Timeframe timeframe) {
        List<CrudeOHLCVData> data = ohlcvRepository.findBySymbolAndTimeframeOrderByTimestampDesc(symbol, timeframe);
        return data.size();
    }

    /**
     * Check data availability for a timeframe
     */
    public boolean hasMinimumData(String symbol, CrudeOHLCVData.Timeframe timeframe, int minRecords) {
        long count = getOHLCVCount(symbol, timeframe);
        return count >= minRecords;
    }
}

