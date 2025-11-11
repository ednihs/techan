package com.stockanalyzer.service;

import com.stockanalyzer.dto.BTSTCandidateDTO;
import com.stockanalyzer.entity.BTSTAnalysis;
import com.stockanalyzer.entity.PriceData;
import com.stockanalyzer.entity.TechnicalIndicator;
import com.stockanalyzer.repository.BTSTAnalysisRepository;
import com.stockanalyzer.repository.PriceDataRepository;
import com.stockanalyzer.repository.TechnicalIndicatorRepository;
import com.stockanalyzer.service.RiskAssessmentService.GapRisk;
import com.stockanalyzer.service.RiskAssessmentService.LiquidityRisk;
import com.stockanalyzer.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class BTSTAnalysisService {

    private final PriceDataRepository priceDataRepository;
    private final TechnicalIndicatorRepository technicalIndicatorRepository;
    private final BTSTAnalysisRepository btstAnalysisRepository;
    private final Executor analysisExecutor = Executors.newFixedThreadPool(8);

    private final Map<LocalDate, List<String>> dayOneCandidateCache = new ConcurrentHashMap<>();

    @Value("${btst.analysis.min-volume:500000}")
    private long minVolume;

    @Value("${btst.analysis.min-value:50000000}")
    private long minValue;

    @Value("${btst.analysis.confidence-threshold:50}")
    private double confidenceThreshold;

    @Scheduled(cron = "0 0 18 * * MON-FRI", zone = "Asia/Kolkata")
    @Transactional
    public void runDailyAnalysis() {
        executeAnalysis(LocalDate.now());
    }

    @Transactional
    public List<BTSTAnalysis> runAnalysis(LocalDate date) {
        LocalDate resolvedDate = DateUtils.resolveDateOrDefault(date, LocalDate.now());
        return executeAnalysis(resolvedDate);
    }

    private List<BTSTAnalysis> executeAnalysis(LocalDate analysisDate) {
        LocalDate previousDate = DateUtils.getPreviousTradingDay(analysisDate);
        log.info("Running BTST analysis for {}", analysisDate);
        List<String> candidates = identifyDay1Candidates(previousDate);
        List<CompletableFuture<BTSTAnalysis>> futures = candidates.stream()
                .map(symbol -> CompletableFuture.supplyAsync(() -> analyze(symbol, previousDate, analysisDate), analysisExecutor)
                        .exceptionally(ex -> {
                            log.error("Failed BTST analysis for {}: {}", symbol, ex.getMessage());
                            return null;
                        }))
                .collect(Collectors.toList());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        List<BTSTAnalysis> results = futures.stream()
                .map(CompletableFuture::join)
                .filter(result -> result != null && result.getConfidenceScore() != null && result.getConfidenceScore() >= confidenceThreshold)
                .sorted((a, b) -> Double.compare(Optional.ofNullable(b.getConfidenceScore()).orElse(0.0), Optional.ofNullable(a.getConfidenceScore()).orElse(0.0)))
                .collect(Collectors.toList());
        btstAnalysisRepository.saveAll(results);
        log.info("Stored {} BTST analyses", results.size());
        return results;
    }

    public List<String> identifyDay1Candidates(LocalDate date) {
        LocalDate endDate = date.minusDays(1);
        LocalDate startDate = endDate.minusDays(5);
        double minAverageValue = 10000000; // 5 Crore

        return priceDataRepository
                .findByTradeDateWithAverageValueTradedFilter(date, startDate, endDate, minValue)
                .stream()
                .filter(priceData -> qualifiesAsDay1Candidate(priceData, date))
                .map(PriceData::getSymbol)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> identifyAndStoreBTSTCandidates(LocalDate date) {
        List<String> candidates = identifyDay1Candidates(date);
        dayOneCandidateCache.put(date, candidates);
        return candidates;
    }

    public List<String> getYesterdayBTSTCandidates() {
        LocalDate today = LocalDate.now();
        LocalDate previous = DateUtils.getPreviousTradingDay(today);
        if (previous == null) {
            return Collections.emptyList();
        }
        return dayOneCandidateCache.computeIfAbsent(previous, this::identifyDay1Candidates);
    }

    public void generateRealtimeRecommendations(LocalDate date) {
        // This method is no longer used as realtimeAnalysisRepository is removed.
        // Keeping it for now as it might be re-introduced or removed later.
    }

    public void refreshTopRecommendations(LocalDate date) {
        // This method is no longer used as realtimeAnalysisRepository is removed.
        // Keeping it for now as it might be re-introduced or removed later.
    }

    public void validateRealtimeSignals(LocalDate date) {
        log.info("Validating realtime signals for {}", date);
    }

    private boolean qualifiesAsDay1Candidate(PriceData priceData, LocalDate date) {
        Optional<TechnicalIndicator> indicator = technicalIndicatorRepository.findBySymbolAndCalculationDate(priceData.getSymbol(), date);
        if (indicator.isEmpty()) {
            return false;
        }
        TechnicalIndicator ti = indicator.get();

        if (priceData.getPrevClosePrice() == null || priceData.getPrevClosePrice().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        double priceChange = priceData.getClosePrice().subtract(priceData.getPrevClosePrice())
                .divide(priceData.getPrevClosePrice(), 4, java.math.RoundingMode.HALF_UP).doubleValue();

        boolean hasBreakout = priceDataRepository.findTop20BySymbolAndTradeDateLessThanOrderByTradeDateDesc(priceData.getSymbol(), date)
                .stream()
                .map(PriceData::getHighPrice)
                .mapToDouble(BigDecimal::doubleValue)
                .max()
                .orElse(0) * 0.98 <= priceData.getHighPrice().doubleValue();

        BigDecimal dayRange = priceData.getHighPrice().subtract(priceData.getLowPrice());
        boolean lateStrength = false;
        if (dayRange.compareTo(BigDecimal.ZERO) > 0) {
            lateStrength = priceData.getHighPrice().subtract(priceData.getClosePrice())
                    .divide(dayRange, 4, java.math.RoundingMode.HALF_UP)
                    .doubleValue() < 0.3;
        }

        return priceChange > 0.02 && hasBreakout && lateStrength && Optional.ofNullable(ti.getVolumeRatio()).orElse(0d) >= 1.5;
    }

    public BTSTAnalysis analyze(String symbol, LocalDate day1, LocalDate day2) {
        Optional<PriceData> day1DataOpt = priceDataRepository.findBySymbolAndTradeDate(symbol, day1);
        Optional<PriceData> day2DataOpt = priceDataRepository.findBySymbolAndTradeDate(symbol, day2);
        if (day1DataOpt.isEmpty() || day2DataOpt.isEmpty()) {
            return null;
        }
        PriceData day1Data = day1DataOpt.get();
        PriceData day2Data = day2DataOpt.get();
        BTSTAnalysis analysis = btstAnalysisRepository.findBySymbolAndAnalysisDate(symbol, day2)
                .orElseGet(BTSTAnalysis::new);
        analysis.setSymbol(symbol);
        analysis.setAnalysisDate(day2);
        populateDayOneCharacteristics(analysis, day1Data, day1);
        populateDayTwoIndicators(analysis, day1Data, day2Data);
        computeTechnicalSetup(analysis, symbol, day2);
        scoreRecommendation(analysis, day2Data);
        LiquidityRisk liquidityRisk = null; // RiskAssessmentService is removed
        GapRisk gapRisk = null; // RiskAssessmentService is removed
        analysis.setLiquidityRiskLevel(liquidityRisk != null ? liquidityRisk.getLevel() : null);
        analysis.setLiquidityRiskFactors(liquidityRisk != null ? liquidityRisk.getFactors() : null);
        analysis.setGapRiskLevel(gapRisk != null ? gapRisk.getLevel() : null);
        analysis.setGapRiskFactors(gapRisk != null ? gapRisk.getFactors() : null);
        analysis.setAutomatedRiskAssessment(Boolean.TRUE);
        if (analysis.getEntryPrice() != null && analysis.getTargetPrice() != null && analysis.getStopLoss() != null) {
            analysis.setPositionSizePercent(0.75);
            analysis.setRiskRewardRatioT1(computeRiskReward(analysis.getEntryPrice(), analysis.getTargetPrice(), analysis.getStopLoss()));
            analysis.setRiskRewardRatioT2(computeRiskReward(analysis.getEntryPrice(), analysis.getTargetPrice() * 1.02, analysis.getStopLoss()));
        }
        return analysis;
    }

    private void populateDayOneCharacteristics(BTSTAnalysis analysis, PriceData day1Data, LocalDate day1) {
        BigDecimal range = day1Data.getHighPrice().subtract(day1Data.getLowPrice());
        BigDecimal distanceFromHigh = day1Data.getHighPrice().subtract(day1Data.getClosePrice());
        boolean hadLateSurge = range.doubleValue() > 0 && distanceFromHigh.divide(range, 4, java.math.RoundingMode.HALF_UP).doubleValue() < 0.3;
        analysis.setHadLateSurge(hadLateSurge);
        technicalIndicatorRepository.findBySymbolAndCalculationDate(analysis.getSymbol(), day1)
                .ifPresent(indicator -> analysis.setLateSessionVolumeRatio(indicator.getVolumeRatio()));
        double breakoutLevel = priceDataRepository.findTop20BySymbolAndTradeDateLessThanOrderByTradeDateDesc(analysis.getSymbol(), day1)
                .stream()
                .map(PriceData::getHighPrice)
                .mapToDouble(BigDecimal::doubleValue)
                .max()
                .orElse(day1Data.getHighPrice().doubleValue());
        analysis.setBreakoutLevel(breakoutLevel);
        analysis.setHadCatalyst(Boolean.FALSE);
        analysis.setCatalystType("UNKNOWN");
    }

    private void populateDayTwoIndicators(BTSTAnalysis analysis, PriceData day1Data, PriceData day2Data) {
        double gap = day2Data.getOpenPrice().subtract(day1Data.getClosePrice())
                .divide(day1Data.getClosePrice(), 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100;
        analysis.setGapPercentage(gap);
        boolean absorption = gap < 0 && gap > -1.5 && day2Data.getClosePrice().doubleValue() > day2Data.getOpenPrice().doubleValue();
        analysis.setShowsAbsorption(absorption);
        double avgTradeSize = Optional.ofNullable(day2Data.getNoOfTrades()).orElse(0) == 0 ? 0 :
                (double) Optional.ofNullable(day2Data.getVolume()).orElse(0L) / Optional.ofNullable(day2Data.getNoOfTrades()).orElse(1);
        analysis.setAverageTradeSize(avgTradeSize);
        analysis.setRetailIntensity(50.0);
        analysis.setVwapReclaimed(Boolean.TRUE);
        analysis.setCumulativeDelta(0.0);
        analysis.setPullbackDepth(Math.abs(gap));
        analysis.setSupplyExhaustion(absorption);
    }

    private void computeTechnicalSetup(BTSTAnalysis analysis, String symbol, LocalDate day2) {
        technicalIndicatorRepository.findBySymbolAndCalculationDate(symbol, day2).ifPresent(indicator -> {
            double strength = Optional.ofNullable(indicator.getPriceStrength()).orElse(0.0)
                    + Optional.ofNullable(indicator.getVolumeStrength()).orElse(0.0);
            analysis.setStrengthScore(strength);
        });
    }

    private void scoreRecommendation(BTSTAnalysis analysis, PriceData latestPrice) {
        double score = Optional.ofNullable(analysis.getStrengthScore()).orElse(0.0);
        if (Boolean.TRUE.equals(analysis.getShowsAbsorption())) {
            score += 15;
        }
        if (Boolean.TRUE.equals(analysis.getSupplyExhaustion())) {
            score += 10;
        }
        analysis.setConfidenceScore(score);
        if (score >= 70) {
            analysis.setRecommendation("BUY");
            analysis.setEntryPrice(latestPrice.getClosePrice().doubleValue());
            analysis.setTargetPrice(latestPrice.getClosePrice().doubleValue() * 1.03);
            analysis.setStopLoss(latestPrice.getClosePrice().doubleValue() * 0.985);
        } else if (score >= 50) {
            analysis.setRecommendation("HOLD");
            analysis.setEntryPrice(latestPrice.getClosePrice().doubleValue() * 0.995);
            analysis.setTargetPrice(latestPrice.getClosePrice().doubleValue() * 1.02);
            analysis.setStopLoss(latestPrice.getClosePrice().doubleValue() * 0.98);
        } else {
            analysis.setRecommendation("AVOID");
        }
    }

    public List<BTSTCandidateDTO> buildCandidateSnapshot(LocalDate date) {
        LocalDate resolvedDate = DateUtils.resolveDateOrDefault(date, LocalDate.now());
        LocalDate endDate = resolvedDate.minusDays(1);
        LocalDate startDate = endDate.minusDays(5);
        double minAverageValue = 10000000.0; // 1 Crore

        List<PriceData> candidates = priceDataRepository
                .findByTradeDateWithAverageValueTradedFilter(resolvedDate, startDate, endDate, minValue);
        List<BTSTCandidateDTO> result = new ArrayList<>();
        for (PriceData candidate : candidates) {
            technicalIndicatorRepository.findBySymbolAndCalculationDate(candidate.getSymbol(), resolvedDate)
                    .ifPresent(indicator -> result.add(BTSTCandidateDTO.builder()
                            .symbol(candidate.getSymbol())
                            .tradeDate(resolvedDate)
                            .closePrice(candidate.getClosePrice().doubleValue())
                            .breakoutLevel(Optional.ofNullable(indicator.getSma20()).orElse(0.0))
                            .volumeRatio(Optional.ofNullable(indicator.getVolumeRatio()).orElse(0.0))
                            .strengthScore(Optional.ofNullable(indicator.getPriceStrength()).orElse(0.0))
                            .breakoutConfirmed(qualifiesAsDay1Candidate(candidate, resolvedDate))
                            .build()));
        }
        return result;
    }

    private Double computeRiskReward(Double entry, Double target, Double stop) {
        double risk = entry - stop;
        if (risk <= 0) {
            return null;
        }
        double reward = target - entry;
        return reward / risk;
    }
}
