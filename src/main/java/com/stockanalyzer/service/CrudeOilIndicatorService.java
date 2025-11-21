package com.stockanalyzer.service;

import com.stockanalyzer.dto.CrudeIndicatorCalculationRequest;
import com.stockanalyzer.dto.CrudeIndicatorCalculationResponse;
import com.stockanalyzer.dto.ConsolidatedCrudeIndicatorDTO;
import com.stockanalyzer.dto.LatestCrudeIndicatorDTO;
import com.stockanalyzer.entity.CrudeOHLCVData;
import com.stockanalyzer.entity.CrudeTechnicalIndicator;
import com.stockanalyzer.repository.CrudeOHLCVDataRepository;
import com.stockanalyzer.repository.CrudeTechnicalIndicatorRepository;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MAType;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating and managing crude oil technical indicators.
 * Leverages existing indicator calculation logic from TechnicalAnalysisService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CrudeOilIndicatorService {

    private final CrudeOHLCVDataRepository ohlcvRepository;
    private final CrudeTechnicalIndicatorRepository indicatorRepository;
    private final VolumeIndicatorService volumeIndicatorService;
    private final Core taLib = new Core();

    @Value("${crude.indicators.min-candles:200}")
    private int minCandles;

    @Value("${crude.vwap.reset-daily:true}")
    private boolean vwapResetDaily;

    /**
     * Main method to calculate indicators for crude oil data
     */
    @Transactional
    public CrudeIndicatorCalculationResponse calculateIndicators(CrudeIndicatorCalculationRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Parse timeframes
            List<CrudeOHLCVData.Timeframe> timeframes = parseTimeframes(request.getTimeframes());
            
            // Set default date range if not provided
            LocalDateTime endDate = request.getEndDate() != null ? request.getEndDate() : LocalDateTime.now();
            LocalDateTime startDate = request.getStartDate() != null ? request.getStartDate() : endDate.minusDays(60);

            Map<String, Integer> processedCounts = new HashMap<>();
            int totalProcessed = 0;

            for (CrudeOHLCVData.Timeframe timeframe : timeframes) {
                int count = processTimeframe(request.getSymbol(), timeframe, startDate, endDate, request.isRecalculate());
                processedCounts.put(timeframe.getValue(), count);
                totalProcessed += count;
            }

            long executionTime = System.currentTimeMillis() - startTime;

            CrudeIndicatorCalculationResponse.CalculationData data = CrudeIndicatorCalculationResponse.CalculationData.builder()
                    .processed(processedCounts)
                    .totalRecords(totalProcessed)
                    .executionTimeMs(executionTime)
                    .build();

            return CrudeIndicatorCalculationResponse.success("Indicators calculated successfully", data);

        } catch (Exception e) {
            log.error("Error calculating crude oil indicators", e);
            return CrudeIndicatorCalculationResponse.error(
                    "Failed to calculate indicators",
                    e.getMessage()
            );
        }
    }

    /**
     * Process indicators for a specific timeframe
     */
    private int processTimeframe(String symbol, CrudeOHLCVData.Timeframe timeframe,
                                  LocalDateTime startDate, LocalDateTime endDate,
                                  boolean recalculate) {
        // Fetch OHLCV data
        List<CrudeOHLCVData> candles = ohlcvRepository.findBySymbolAndTimeframeAndTimestampBetweenOrderByTimestampAsc(
                symbol, timeframe, startDate, endDate
        );

        if (candles.size() < minCandles) {
            log.warn("Insufficient data for timeframe {}. Required: {}, Available: {}", 
                    timeframe, minCandles, candles.size());
            return 0;
        }

        int processed = 0;
        
        // Prepare arrays for technical analysis
        double[] close = candles.stream().map(c -> c.getClose().doubleValue()).mapToDouble(Double::doubleValue).toArray();
        double[] high = candles.stream().map(c -> c.getHigh().doubleValue()).mapToDouble(Double::doubleValue).toArray();
        double[] low = candles.stream().map(c -> c.getLow().doubleValue()).mapToDouble(Double::doubleValue).toArray();

        // Volume indicators need to be calculated iteratively
        List<Long> obvValues = new ArrayList<>();
        List<BigDecimal> pvtValues = new ArrayList<>();
        
        for (int i = 0; i < candles.size(); i++) {
            CrudeOHLCVData candle = candles.get(i);

            // Skip if already exists and not recalculating
            if (!recalculate && indicatorRepository.existsByOhlcvId(candle.getId())) {
                continue;
            }

            CrudeTechnicalIndicator indicator = calculateIndicatorsForCandle(candles, i, close, high, low, obvValues, pvtValues);
            indicator.setOhlcvId(candle.getId());
            indicator.setTimestamp(candle.getTimestamp());
            indicator.setTimeframe(timeframe);

            indicatorRepository.save(indicator);
            processed++;
        }

        return processed;
    }

    /**
     * Calculate all indicators for a single candle
     */
    private CrudeTechnicalIndicator calculateIndicatorsForCandle(
            List<CrudeOHLCVData> candles, int index,
            double[] close, double[] high, double[] low,
            List<Long> obvValues, List<BigDecimal> pvtValues) {

        CrudeTechnicalIndicator indicator = new CrudeTechnicalIndicator();
        CrudeOHLCVData currentCandle = candles.get(index);

        // Price-based indicators
        calculatePriceIndicators(indicator, close, high, low, index);

        // Moving averages
        calculateMovingAverages(indicator, close, index, currentCandle.getClose());

        // Support/Resistance
        calculateSupportResistance(indicator, candles, index);

        // Volume indicators
        calculateVolumeIndicators(indicator, candles, index, obvValues, pvtValues);

        // Metadata
        indicator.setDataQualityFlag(determineDataQuality(currentCandle.getTimestamp()));
        indicator.setDayType(CrudeTechnicalIndicator.DayType.Regular); // Can be enhanced with event calendar

        return indicator;
    }

    /**
     * Calculate price-based indicators (RSI, MACD, Bollinger, ATR)
     */
    private void calculatePriceIndicators(CrudeTechnicalIndicator indicator, 
                                         double[] close, double[] high, double[] low, int index) {
        // RSI
        if (index >= 13) {
            double[] rsiOut = new double[close.length];
            MInteger outBeg = new MInteger();
            MInteger outNb = new MInteger();
            if (taLib.rsi(0, index, close, 14, outBeg, outNb, rsiOut) == RetCode.Success && outNb.value > 0) {
                indicator.setRsi14(BigDecimal.valueOf(rsiOut[outNb.value - 1]).setScale(2, RoundingMode.HALF_UP));
            }
        }

        // MACD
        if (index >= 33) {
            double[] macd = new double[close.length];
            double[] signal = new double[close.length];
            double[] hist = new double[close.length];
            MInteger outBeg = new MInteger();
            MInteger outNb = new MInteger();
            if (taLib.macd(0, index, close, 12, 26, 9, outBeg, outNb, macd, signal, hist) == RetCode.Success && outNb.value > 0) {
                int i = outNb.value - 1;
                indicator.setMacdLine(BigDecimal.valueOf(macd[i]).setScale(4, RoundingMode.HALF_UP));
                indicator.setMacdSignal(BigDecimal.valueOf(signal[i]).setScale(4, RoundingMode.HALF_UP));
                indicator.setMacdHistogram(BigDecimal.valueOf(hist[i]).setScale(4, RoundingMode.HALF_UP));
                
                // Determine crossover signal
                if (hist[i] > 0 && (i == 0 || hist[i - 1] <= 0)) {
                    indicator.setMacdCrossoverSignal(CrudeTechnicalIndicator.MacdSignal.bullish);
                } else if (hist[i] < 0 && (i == 0 || hist[i - 1] >= 0)) {
                    indicator.setMacdCrossoverSignal(CrudeTechnicalIndicator.MacdSignal.bearish);
                } else {
                    indicator.setMacdCrossoverSignal(CrudeTechnicalIndicator.MacdSignal.neutral);
                }
            }
        }

        // Bollinger Bands
        if (index >= 19) {
            double[] upper = new double[close.length];
            double[] middle = new double[close.length];
            double[] lower = new double[close.length];
            MInteger outBeg = new MInteger();
            MInteger outNb = new MInteger();
            if (taLib.bbands(0, index, close, 20, 2.0, 2.0, MAType.Sma, outBeg, outNb, upper, middle, lower) == RetCode.Success && outNb.value > 0) {
                int i = outNb.value - 1;
                indicator.setBbUpper(BigDecimal.valueOf(upper[i]).setScale(4, RoundingMode.HALF_UP));
                indicator.setBbMiddle(BigDecimal.valueOf(middle[i]).setScale(4, RoundingMode.HALF_UP));
                indicator.setBbLower(BigDecimal.valueOf(lower[i]).setScale(4, RoundingMode.HALF_UP));
                indicator.setBbWidth(BigDecimal.valueOf((upper[i] - lower[i]) / middle[i]).setScale(4, RoundingMode.HALF_UP));
            }
        }

        // ATR
        if (index >= 13) {
            double[] atr = new double[close.length];
            MInteger outBeg = new MInteger();
            MInteger outNb = new MInteger();
            if (taLib.atr(0, index, high, low, close, 14, outBeg, outNb, atr) == RetCode.Success && outNb.value > 0) {
                indicator.setAtr14(BigDecimal.valueOf(atr[outNb.value - 1]).setScale(4, RoundingMode.HALF_UP));
            }
        }
    }

    /**
     * Calculate moving averages
     */
    private void calculateMovingAverages(CrudeTechnicalIndicator indicator, double[] close, int index, BigDecimal currentPrice) {
        // Calculate SMAs
        int[] smaPeriods = {20, 50, 100, 200};
        BigDecimal[] smaValues = volumeIndicatorService.calculateMultipleSMAs(
                Arrays.copyOfRange(close, 0, index + 1), smaPeriods
        );

        if (smaValues[0] != null) {
            indicator.setSma20(smaValues[0]);
            indicator.setPriceVsSma20(volumeIndicatorService.calculatePriceVsSMA(currentPrice, smaValues[0]));
        }
        if (smaValues[1] != null) {
            indicator.setSma50(smaValues[1]);
            indicator.setPriceVsSma50(volumeIndicatorService.calculatePriceVsSMA(currentPrice, smaValues[1]));
        }
        if (smaValues[2] != null) {
            indicator.setSma100(smaValues[2]);
            indicator.setPriceVsSma100(volumeIndicatorService.calculatePriceVsSMA(currentPrice, smaValues[2]));
        }
        if (smaValues[3] != null) {
            indicator.setSma200(smaValues[3]);
            indicator.setPriceVsSma200(volumeIndicatorService.calculatePriceVsSMA(currentPrice, smaValues[3]));
        }

        // Calculate EMAs
        if (index >= 11) {
            double[] ema12 = new double[close.length];
            MInteger outBeg = new MInteger();
            MInteger outNb = new MInteger();
            if (taLib.ema(0, index, close, 12, outBeg, outNb, ema12) == RetCode.Success && outNb.value > 0) {
                indicator.setEma12(BigDecimal.valueOf(ema12[outNb.value - 1]).setScale(4, RoundingMode.HALF_UP));
            }
        }

        if (index >= 25) {
            double[] ema26 = new double[close.length];
            MInteger outBeg = new MInteger();
            MInteger outNb = new MInteger();
            if (taLib.ema(0, index, close, 26, outBeg, outNb, ema26) == RetCode.Success && outNb.value > 0) {
                indicator.setEma26(BigDecimal.valueOf(ema26[outNb.value - 1]).setScale(4, RoundingMode.HALF_UP));
            }
        }
    }

    /**
     * Calculate support and resistance levels
     */
    private void calculateSupportResistance(CrudeTechnicalIndicator indicator, List<CrudeOHLCVData> candles, int index) {
        if (index >= 19) {
            BigDecimal highest = volumeIndicatorService.findHighest(candles, index, 20);
            BigDecimal lowest = volumeIndicatorService.findLowest(candles, index, 20);
            
            indicator.setHighest20(highest);
            indicator.setLowest20(lowest);

            // Simple support/resistance calculation
            BigDecimal range = highest.subtract(lowest);
            BigDecimal pivot = highest.add(lowest).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
            
            indicator.setResistance1(pivot.add(range.multiply(BigDecimal.valueOf(0.382))));
            indicator.setResistance2(pivot.add(range.multiply(BigDecimal.valueOf(0.618))));
            indicator.setSupport1(pivot.subtract(range.multiply(BigDecimal.valueOf(0.382))));
            indicator.setSupport2(pivot.subtract(range.multiply(BigDecimal.valueOf(0.618))));
        }
    }

    /**
     * Calculate volume-based indicators
     */
    private void calculateVolumeIndicators(CrudeTechnicalIndicator indicator, List<CrudeOHLCVData> candles, 
                                          int index, List<Long> obvValues, List<BigDecimal> pvtValues) {
        Long previousOBV = obvValues.isEmpty() ? null : obvValues.get(obvValues.size() - 1);
        BigDecimal previousPVT = pvtValues.isEmpty() ? null : pvtValues.get(pvtValues.size() - 1);

        // OBV
        long obv = volumeIndicatorService.calculateOBV(candles, index, previousOBV);
        obvValues.add(obv);
        indicator.setObv(obv);

        // OBV EMA (10-period)
        if (obvValues.size() >= 10) {
            BigDecimal obvEma = volumeIndicatorService.calculateOBVEMA(obvValues, 10);
            indicator.setObvEma(obvEma);
        }

        // VWAP
        BigDecimal vwap = volumeIndicatorService.calculateVWAP(candles, index, vwapResetDaily);
        indicator.setVwap(vwap);

        // Volume SMA and Ratio
        if (index >= 19) {
            long volumeSma = volumeIndicatorService.calculateVolumeSMA(candles, index, 20);
            indicator.setVolumeSma20(volumeSma);
            
            BigDecimal volumeRatio = volumeIndicatorService.calculateVolumeRatio(
                    candles.get(index).getVolume(), volumeSma
            );
            indicator.setVolumeRatio(volumeRatio);
        }

        // PVT
        BigDecimal pvt = volumeIndicatorService.calculatePriceVolumeTrend(candles, index, previousPVT);
        pvtValues.add(pvt);
        indicator.setPriceVolumeTrend(pvt);

        // Volume ROC
        BigDecimal volumeRoc = volumeIndicatorService.calculateVolumeRateOfChange(candles, index);
        indicator.setVolumeRateOfChange(volumeRoc);
    }

    /**
     * Determine data quality based on timestamp
     */
    private CrudeTechnicalIndicator.DataQualityFlag determineDataQuality(LocalDateTime timestamp) {
        Duration age = Duration.between(timestamp, LocalDateTime.now());
        
        if (age.toHours() <= 1) {
            return CrudeTechnicalIndicator.DataQualityFlag.Fresh;
        } else if (age.toHours() <= 24) {
            return CrudeTechnicalIndicator.DataQualityFlag.Recent;
        } else {
            return CrudeTechnicalIndicator.DataQualityFlag.Stale;
        }
    }

    /**
     * Parse timeframe strings to enum values
     */
    private List<CrudeOHLCVData.Timeframe> parseTimeframes(List<String> timeframeStrings) {
        if (timeframeStrings == null || timeframeStrings.isEmpty()) {
            return Arrays.asList(CrudeOHLCVData.Timeframe.values());
        }

        return timeframeStrings.stream()
                .map(CrudeOHLCVData.Timeframe::fromValue)
                .collect(Collectors.toList());
    }

    /**
     * Get consolidated data for export
     */
    public List<ConsolidatedCrudeIndicatorDTO> getConsolidatedData(
            List<String> timeframeStrings,
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<String> dataQualityFlags) {

        List<CrudeOHLCVData.Timeframe> timeframes = parseTimeframes(timeframeStrings);
        
        // Set defaults
        if (endDate == null) endDate = LocalDateTime.now();
        if (startDate == null) startDate = endDate.minusDays(30);

        // Fetch OHLCV data
        List<CrudeOHLCVData> ohlcvData = ohlcvRepository.findBySymbolAndTimeframesAndDateRange(
                "BRN", timeframes, startDate, endDate
        );

        // Fetch indicators
        List<CrudeTechnicalIndicator> indicators = indicatorRepository.findByTimeframesAndDateRange(
                timeframes, startDate, endDate
        );

        // Create map for quick lookup
        Map<Long, CrudeTechnicalIndicator> indicatorMap = indicators.stream()
                .collect(Collectors.toMap(CrudeTechnicalIndicator::getOhlcvId, i -> i));

        // Build consolidated DTOs
        return ohlcvData.stream()
                .map(ohlcv -> {
                    CrudeTechnicalIndicator indicator = indicatorMap.get(ohlcv.getId());
                    if (indicator == null) return null;

                    // Filter by data quality if specified
                    if (dataQualityFlags != null && !dataQualityFlags.isEmpty()) {
                        if (!dataQualityFlags.contains(indicator.getDataQualityFlag().name())) {
                            return null;
                        }
                    }

                    return buildConsolidatedDTO(ohlcv, indicator);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Build consolidated DTO from OHLCV and indicator data
     */
    private ConsolidatedCrudeIndicatorDTO buildConsolidatedDTO(CrudeOHLCVData ohlcv, CrudeTechnicalIndicator indicator) {
        return ConsolidatedCrudeIndicatorDTO.builder()
                .timestamp(ohlcv.getTimestamp())
                .timeframe(ohlcv.getTimeframe().getValue())
                .open(ohlcv.getOpen())
                .high(ohlcv.getHigh())
                .low(ohlcv.getLow())
                .close(ohlcv.getClose())
                .volume(ohlcv.getVolume())
                .rsi14(indicator.getRsi14())
                .macdLine(indicator.getMacdLine())
                .macdSignal(indicator.getMacdSignal())
                .macdHistogram(indicator.getMacdHistogram())
                .macdCrossoverSignal(indicator.getMacdCrossoverSignal() != null ? indicator.getMacdCrossoverSignal().name() : null)
                .bbUpper(indicator.getBbUpper())
                .bbMiddle(indicator.getBbMiddle())
                .bbLower(indicator.getBbLower())
                .bbWidth(indicator.getBbWidth())
                .atr14(indicator.getAtr14())
                .sma20(indicator.getSma20())
                .sma50(indicator.getSma50())
                .sma100(indicator.getSma100())
                .sma200(indicator.getSma200())
                .priceVsSma20(indicator.getPriceVsSma20())
                .priceVsSma50(indicator.getPriceVsSma50())
                .priceVsSma100(indicator.getPriceVsSma100())
                .priceVsSma200(indicator.getPriceVsSma200())
                .highest20(indicator.getHighest20())
                .lowest20(indicator.getLowest20())
                .support1(indicator.getSupport1())
                .support2(indicator.getSupport2())
                .resistance1(indicator.getResistance1())
                .resistance2(indicator.getResistance2())
                .obv(indicator.getObv())
                .obvEma(indicator.getObvEma())
                .vwap(indicator.getVwap())
                .volumeSma20(indicator.getVolumeSma20())
                .volumeRatio(indicator.getVolumeRatio())
                .priceVolumeTrend(indicator.getPriceVolumeTrend())
                .volumeRateOfChange(indicator.getVolumeRateOfChange())
                .dataQualityFlag(indicator.getDataQualityFlag().name())
                .dayType(indicator.getDayType() != null ? indicator.getDayType().name() : null)
                .build();
    }

    /**
     * Get latest indicators for real-time analysis
     */
    public Map<String, List<LatestCrudeIndicatorDTO>> getLatestIndicators(List<String> timeframeStrings, int limit) {
        List<CrudeOHLCVData.Timeframe> timeframes = parseTimeframes(timeframeStrings);
        Map<String, List<LatestCrudeIndicatorDTO>> result = new HashMap<>();

        for (CrudeOHLCVData.Timeframe timeframe : timeframes) {
            List<CrudeTechnicalIndicator> indicators = indicatorRepository.findLatestByTimeframe(timeframe, limit);
            
            List<LatestCrudeIndicatorDTO> dtoList = indicators.stream()
                    .map(ind -> {
                        // Fetch corresponding OHLCV data
                        return ohlcvRepository.findById(ind.getOhlcvId())
                                .map(ohlcv -> LatestCrudeIndicatorDTO.builder()
                                        .timestamp(ind.getTimestamp())
                                        .close(ohlcv.getClose())
                                        .rsi14(ind.getRsi14())
                                        .macdCrossoverSignal(ind.getMacdCrossoverSignal() != null ? ind.getMacdCrossoverSignal().name() : null)
                                        .obv(ind.getObv())
                                        .vwap(ind.getVwap())
                                        .volumeRatio(ind.getVolumeRatio())
                                        .dataQualityFlag(ind.getDataQualityFlag().name())
                                        .build())
                                .orElse(null);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            result.put(timeframe.getValue(), dtoList);
        }

        return result;
    }
}

