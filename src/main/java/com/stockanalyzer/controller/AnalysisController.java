package com.stockanalyzer.controller;

import com.stockanalyzer.dto.BTSTCandidateDTO;
import com.stockanalyzer.dto.BTSTDetailedAnalysisDTO;
import com.stockanalyzer.dto.BTSTRecommendationDTO;
import com.stockanalyzer.dto.RealtimeBTSTSignalDTO;
import com.stockanalyzer.dto.TechnicalIndicatorDTO;
import com.stockanalyzer.entity.BTSTAnalysis;
import com.stockanalyzer.entity.TechnicalIndicator;
import com.stockanalyzer.repository.BTSTAnalysisRepository;
import com.stockanalyzer.repository.TechnicalIndicatorRepository;
import com.stockanalyzer.service.BTSTAnalysisService;
import com.stockanalyzer.service.RealtimeAnalysisRepository;
import com.stockanalyzer.service.RealtimeWeakHandsService;
import com.stockanalyzer.service.RiskAssessmentService;
import com.stockanalyzer.service.RiskAssessmentService.GapRisk;
import com.stockanalyzer.service.RiskAssessmentService.LiquidityRisk;
import com.stockanalyzer.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@Slf4j
public class AnalysisController {

    private final BTSTAnalysisService btstAnalysisService;
    private final BTSTAnalysisRepository btstAnalysisRepository;
    private final TechnicalIndicatorRepository technicalIndicatorRepository;
    private final RiskAssessmentService riskAssessmentService;
    private final RealtimeAnalysisRepository realtimeAnalysisRepository;

    @GetMapping("/btst/recommendations")
    public ResponseEntity<List<BTSTRecommendationDTO>> getRecommendations(
            @RequestParam(defaultValue = "1970-01-01") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "BUY") String recommendation) {
        LocalDate resolvedDate = DateUtils.resolveDateOrDefault(date, LocalDate.now());
        List<BTSTRecommendationDTO> payload = btstAnalysisRepository
                .findByAnalysisDateAndRecommendationOrderByConfidenceScoreDesc(resolvedDate, recommendation)
                .stream()
                .map(this::toRecommendation)
                .collect(Collectors.toList());
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/btst/realtime-signals")
    public ResponseEntity<List<RealtimeBTSTSignalDTO>> getRealtimeSignals(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate analysisDate = date != null ? date : LocalDate.now();
        List<RealtimeWeakHandsService.AfternoonAnalysis> analyses =
                realtimeAnalysisRepository.findAfternoonAnalysesWithBuySignal(analysisDate);
        if (analyses.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<RealtimeBTSTSignalDTO> payload = analyses.stream()
                .map(analysis -> toRealtimeSignalDto(analysis, analysisDate))
                .sorted((a, b) -> Double.compare(b.getWeakHandsScore(), a.getWeakHandsScore()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/btst/run")
    public ResponseEntity<List<BTSTRecommendationDTO>> runOnDemandAnalysis(
            @RequestParam(defaultValue = "1970-01-01") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate resolvedDate = DateUtils.resolveDateOrDefault(date, LocalDate.now());
        List<BTSTRecommendationDTO> payload = btstAnalysisService.runAnalysis(resolvedDate)
                .stream()
                .map(this::toRecommendation)
                .collect(Collectors.toList());
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/btst/detailed/{symbol}")
    public ResponseEntity<BTSTDetailedAnalysisDTO> getDetailedAnalysis(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1970-01-01") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate resolvedDate = DateUtils.resolveDateOrDefault(date, LocalDate.now());
        Optional<BTSTAnalysis> analysisOpt = btstAnalysisRepository.findBySymbolAndAnalysisDate(symbol, resolvedDate);
        if (analysisOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        BTSTAnalysis analysis = analysisOpt.get();
        LiquidityRisk liquidityRisk = riskAssessmentService.calculateLiquidityRisk(symbol, resolvedDate);
        GapRisk gapRisk = riskAssessmentService.calculateGapRisk(symbol, resolvedDate, analysis);
        return ResponseEntity.ok(toDetailedDto(analysis, liquidityRisk, gapRisk));
    }

    @GetMapping("/screening/candidates")
    public ResponseEntity<List<BTSTCandidateDTO>> getCandidates(
            @RequestParam(defaultValue = "1970-01-01") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(btstAnalysisService.buildCandidateSnapshot(date));
    }

    @GetMapping("/technical/{symbol}")
    public ResponseEntity<TechnicalIndicatorDTO> getTechnicalIndicators(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1970-01-01") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate resolvedDate = DateUtils.resolveDateOrDefault(date, LocalDate.now());
        Optional<TechnicalIndicator> indicator = technicalIndicatorRepository.findBySymbolAndCalculationDate(symbol, resolvedDate);
        return indicator.map(this::toTechnicalDto).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private BTSTRecommendationDTO toRecommendation(BTSTAnalysis analysis) {
        return BTSTRecommendationDTO.builder()
                .symbol(analysis.getSymbol())
                .recommendation(analysis.getRecommendation())
                .confidenceScore(analysis.getConfidenceScore())
                .entryPrice(analysis.getEntryPrice())
                .targetPrice(analysis.getTargetPrice())
                .stopLoss(analysis.getStopLoss())
                .strengthScore(analysis.getStrengthScore())
                .liquidityRiskLevel(analysis.getLiquidityRiskLevel())
                .gapRiskLevel(analysis.getGapRiskLevel())
                .build();
    }

    private RealtimeBTSTSignalDTO toRealtimeSignalDto(RealtimeWeakHandsService.AfternoonAnalysis analysis, LocalDate date) {
        RealtimeBTSTSignalDTO dto = new RealtimeBTSTSignalDTO();
        dto.setSymbol(analysis.getSymbol());
        dto.setSector("UNKNOWN");
        dto.setSignalTime(analysis.getAnalysisTime());
        dto.setCurrentPrice(analysis.getCurrentPrice());
        dto.setRetailIntensity(analysis.getRetailIntensity());
        dto.setCumulativeDelta(analysis.getSessionCumulativeDelta());
        dto.setAbsorptionQuality(analysis.isGoodAbsorption());
        dto.setSupplyExhaustion(analysis.isSupplyExhausted());
        dto.setWeakHandsScore(analysis.getWeakHandsScore());
        dto.setEntryPrice(analysis.getEntryPrice());
        dto.setTargetPrice(analysis.getTargetPrice());
        dto.setStopLoss(analysis.getStopLoss());
        dto.setRiskRewardRatio(analysis.getRiskRewardRatio());
        dto.setPositionSizePercent(analysis.getPositionSizePercent());
        RiskAssessmentService.LiquidityRisk liquidityRisk = riskAssessmentService.calculateLiquidityRisk(analysis.getSymbol(), date);
        dto.setLiquidityRisk(formatRisk(liquidityRisk.getLevel(), liquidityRisk.getFactors()));
        RiskAssessmentService.GapRisk gapRisk = riskAssessmentService.calculateGapRisk(analysis.getSymbol(), date, null);
        dto.setGapRisk(formatRisk(gapRisk.getLevel(), gapRisk.getFactors()));
        dto.setConfidenceScore((int) Math.round(Math.min(100, analysis.getWeakHandsScore())));
        return dto;
    }

    private String formatRisk(String level, String factors) {
        if (factors == null || factors.isBlank()) {
            return level;
        }
        return level + " - " + factors;
    }

    private BTSTDetailedAnalysisDTO toDetailedDto(BTSTAnalysis analysis, LiquidityRisk liquidityRisk, GapRisk gapRisk) {
        return BTSTDetailedAnalysisDTO.builder()
                .symbol(analysis.getSymbol())
                .analysisDate(analysis.getAnalysisDate())
                .hadLateSurge(analysis.getHadLateSurge())
                .gapPercentage(analysis.getGapPercentage())
                .showsAbsorption(analysis.getShowsAbsorption())
                .averageTradeSize(analysis.getAverageTradeSize())
                .retailIntensity(analysis.getRetailIntensity())
                .vwapReclaimed(analysis.getVwapReclaimed())
                .cumulativeDelta(analysis.getCumulativeDelta())
                .pullbackDepth(analysis.getPullbackDepth())
                .supplyExhaustion(analysis.getSupplyExhaustion())
                .strengthScore(analysis.getStrengthScore())
                .recommendation(analysis.getRecommendation())
                .confidenceScore(analysis.getConfidenceScore())
                .entryPrice(analysis.getEntryPrice())
                .targetPrice(analysis.getTargetPrice())
                .stopLoss(analysis.getStopLoss())
                .liquidityRiskLevel(liquidityRisk.getLevel())
                .liquidityRiskFactors(liquidityRisk.getFactors())
                .gapRiskLevel(gapRisk.getLevel())
                .gapRiskFactors(gapRisk.getFactors())
                .catalystScore(analysis.getCatalystScore())
                .catalystDetails(analysis.getCatalystDetails())
                .build();
    }

    private TechnicalIndicatorDTO toTechnicalDto(TechnicalIndicator indicator) {
        return TechnicalIndicatorDTO.builder()
                .symbol(indicator.getSymbol())
                .rsi14(indicator.getRsi14())
                .ema9(indicator.getEma9())
                .ema21(indicator.getEma21())
                .sma20(indicator.getSma20())
                .atr14(indicator.getAtr14())
                .vwap(indicator.getVwap())
                .volumeRatio(indicator.getVolumeRatio())
                .priceStrength(indicator.getPriceStrength())
                .volumeStrength(indicator.getVolumeStrength())
                .deliveryStrength(indicator.getDeliveryStrength())
                .build();
    }
}
