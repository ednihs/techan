package com.stockanalyzer.service;

import com.stockanalyzer.entity.IntradayData;
import com.stockanalyzer.entity.PriceData;
import com.stockanalyzer.repository.IntradayDataRepository;
import com.stockanalyzer.repository.PriceDataRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeWeakHandsService {

    private final IntradayDataRepository intradayDataRepository;
    private final PriceDataRepository priceDataRepository;
    private final RealtimeAnalysisRepository realtimeAnalysisRepository;

    @Transactional(readOnly = true)
    public void analyzeMorningSession(String symbol, LocalDate date) {
        List<IntradayData> morningBars = intradayDataRepository
                .findBySymbolAndTradeDateAndTradeTimeBetweenOrderByTradeTimeAsc(symbol, date,
                        LocalTime.of(9, 15), LocalTime.of(10, 0));
        if (morningBars.isEmpty()) {
            log.debug("Skipping morning analysis for {} - no data", symbol);
            return;
        }
        PriceData yesterday = priceDataRepository.findBySymbolAndTradeDate(symbol, date.minusDays(1)).orElse(null);
        if (yesterday == null) {
            return;
        }
        MorningAnalysis analysis = new MorningAnalysis();
        analysis.setSymbol(symbol);
        analysis.setAnalysisDate(date);
        IntradayData firstBar = morningBars.get(0);
        double gapPercent = computeGap(firstBar.getOpenPrice(), yesterday.getClosePrice());
        analysis.setGapPercentage(gapPercent);
        double cumulativeDelta = 0;
        long totalVolume = 0;
        int downBars = 0;
        for (IntradayData bar : morningBars) {
            double delta = priceChange(bar) * volume(bar);
            cumulativeDelta += delta;
            totalVolume += volume(bar);
            if (isDownBar(bar)) {
                downBars++;
            }
        }
        analysis.setMorningDelta(cumulativeDelta);
        analysis.setMorningVolume(totalVolume);
        analysis.setDownBarsCount(downBars);
        boolean panic = gapPercent < -0.5 && cumulativeDelta < 0 && downBars > (morningBars.size() * 0.6);
        analysis.setPanicSellingDetected(panic);
        double avgTradeSizeProxy = totalVolume / (double) Math.max(morningBars.size(), 1);
        analysis.setAvgTradeSize(avgTradeSizeProxy);
        realtimeAnalysisRepository.saveMorningAnalysis(analysis);
        log.info("Morning analysis for {}: gap={}%, delta={}, panic={}",
                symbol, String.format("%.2f", gapPercent), String.format("%.0f", cumulativeDelta), panic);
    }

    @Transactional(readOnly = true)
    public void analyzeMidSession(String symbol, LocalDate date) {
        List<IntradayData> midBars = intradayDataRepository
                .findBySymbolAndTradeDateAndTradeTimeBetweenOrderByTradeTimeAsc(symbol, date,
                        LocalTime.of(10, 0), LocalTime.of(12, 0));
        if (midBars.isEmpty()) {
            return;
        }
        MidSessionAnalysis analysis = new MidSessionAnalysis();
        analysis.setSymbol(symbol);
        analysis.setAnalysisDate(date);
        double delta = midBars.stream().mapToDouble(bar -> priceChange(bar) * volume(bar)).sum();
        analysis.setMidSessionDelta(delta);
        IntradayData first = midBars.get(0);
        IntradayData last = midBars.get(midBars.size() - 1);
        boolean recovering = safeCompare(last.getClosePrice(), first.getOpenPrice()) > 0;
        analysis.setPriceRecovering(recovering);
        double vwap = calculateSessionVWAP(symbol, date, LocalTime.of(9, 15), LocalTime.of(12, 0));
        analysis.setCurrentVwap(vwap);
        boolean aboveVwap = last.getClosePrice() != null && last.getClosePrice().doubleValue() > vwap;
        analysis.setAboveVwap(aboveVwap);
        boolean absorption = delta > 0 && recovering && aboveVwap;
        analysis.setAbsorptionStarted(absorption);
        realtimeAnalysisRepository.saveMidSessionAnalysis(analysis);
        log.info("Mid-session analysis for {}: delta={}, recovering={}, absorption={}",
                symbol, String.format("%.0f", delta), recovering, absorption);
    }

    @Transactional(readOnly = true)
    public void analyzeAfternoonSession(String symbol, LocalDate date) {
        List<IntradayData> allBars = intradayDataRepository
                .findBySymbolAndTradeDateAndTradeTimeBetweenOrderByTradeTimeAsc(symbol, date,
                        LocalTime.of(9, 15), LocalTime.of(14, 0));
        if (allBars.size() < 5) {
            log.debug("Skipping afternoon analysis for {} due to insufficient data", symbol);
            return;
        }
        PriceData yesterday = priceDataRepository.findBySymbolAndTradeDate(symbol, date.minusDays(1)).orElse(null);
        if (yesterday == null) {
            return;
        }
        MorningAnalysis morning = realtimeAnalysisRepository.findMorningAnalysis(symbol, date);
        MidSessionAnalysis mid = realtimeAnalysisRepository.findMidSessionAnalysis(symbol, date);
        AfternoonAnalysis analysis = new AfternoonAnalysis();
        analysis.setSymbol(symbol);
        analysis.setAnalysisDate(date);
        analysis.setAnalysisTime(LocalTime.now());
        IntradayData latest = allBars.get(allBars.size() - 1);
        analysis.setCurrentPrice(value(latest.getClosePrice()));
        double sessionDelta = allBars.stream().mapToDouble(bar -> priceChange(bar) * volume(bar)).sum();
        analysis.setSessionCumulativeDelta(sessionDelta);
        RetailIntensityMetrics metrics = calculateRetailIntensityProxies(allBars, yesterday);
        analysis.setRetailIntensity(metrics.getRetailIntensityScore());
        double sessionVwap = calculateSessionVWAP(symbol, date, LocalTime.of(9, 15), LocalTime.of(14, 0));
        boolean vwapReclaimed = latest.getClosePrice() != null && latest.getClosePrice().doubleValue() > sessionVwap;
        analysis.setVwapReclaimed(vwapReclaimed);
        boolean goodAbsorption = evaluateAbsorptionQuality(morning, mid, sessionDelta, vwapReclaimed);
        analysis.setGoodAbsorption(goodAbsorption);
        boolean supplyExhausted = evaluateSupplyExhaustion(allBars, sessionDelta);
        analysis.setSupplyExhausted(supplyExhausted);
        double weakHandsScore = calculateWeakHandsScore(morning, mid, analysis);
        analysis.setWeakHandsScore(weakHandsScore);
        boolean buy = generateBuySignal(analysis, weakHandsScore);
        analysis.setBuySignal(buy);
        if (buy) {
            calculateTradingLevels(analysis, yesterday, latest);
        }
        realtimeAnalysisRepository.saveAfternoonAnalysis(analysis);
        if (buy) {
            log.info("Buy signal for {} score={} entry={} target={} sl={}",
                    symbol,
                    String.format("%.1f", weakHandsScore),
                    String.format("%.2f", analysis.getEntryPrice()),
                    String.format("%.2f", analysis.getTargetPrice()),
                    String.format("%.2f", analysis.getStopLoss()));
        } else {
            log.debug("No afternoon signal for {} score={}", symbol, String.format("%.1f", weakHandsScore));
        }
    }

    private double calculateSessionVWAP(String symbol, LocalDate date, LocalTime from, LocalTime to) {
        List<IntradayData> bars = intradayDataRepository
                .findBySymbolAndTradeDateAndTradeTimeBetweenOrderByTradeTimeAsc(symbol, date, from, to);
        if (bars.isEmpty()) {
            return 0;
        }
        double totalPV = 0;
        long totalVolume = 0;
        for (IntradayData bar : bars) {
            double typical = typicalPrice(bar);
            long vol = volume(bar);
            totalPV += typical * vol;
            totalVolume += vol;
        }
        return totalVolume == 0 ? 0 : totalPV / totalVolume;
    }

    private RetailIntensityMetrics calculateRetailIntensityProxies(List<IntradayData> bars, PriceData yesterday) {
        RetailIntensityMetrics metrics = new RetailIntensityMetrics();
        metrics.setVolatilityScore(calculateVolatilityProxy(bars));
        metrics.setVolumeClusterScore(calculateVolumeClusteringProxy(bars));
        metrics.setEstimatedDeliveryPct(estimateIntradayDelivery(bars, yesterday));
        metrics.setPriceRejectionScore(calculatePriceRejectionProxy(bars));
        metrics.setVolumePriceCoherence(calculateVolumePriceCoherence(bars));
        metrics.setMomentumExhaustion(calculateMomentumExhaustion(bars));
        metrics.setRetailIntensityScore(calculateCompositeRetailScore(metrics));
        return metrics;
    }

    private double calculateVolatilityProxy(List<IntradayData> bars) {
        if (bars.size() < 2) {
            return 50d;
        }
        double totalVolatility = 0;
        int reversals = 0;
        for (int i = 1; i < bars.size(); i++) {
            IntradayData prev = bars.get(i - 1);
            IntradayData current = bars.get(i);
            double range = Math.abs(value(current.getHighPrice()) - value(current.getLowPrice()));
            double mid = (value(current.getHighPrice()) + value(current.getLowPrice())) / 2d;
            double barVol = mid == 0 ? 0 : (range / mid) * 100;
            totalVolatility += barVol;
            boolean prevBullish = safeCompare(prev.getClosePrice(), prev.getOpenPrice()) > 0;
            boolean currentBullish = safeCompare(current.getClosePrice(), current.getOpenPrice()) > 0;
            if (prevBullish != currentBullish) {
                reversals++;
            }
        }
        double avgVolatility = totalVolatility / bars.size();
        double reversalRate = (double) reversals / bars.size() * 100;
        return Math.min(100, avgVolatility * 20 + reversalRate * 30);
    }

    private double calculateVolumeClusteringProxy(List<IntradayData> bars) {
        if (bars.isEmpty()) {
            return 50d;
        }
        long morningVolume = 0;
        long midVolume = 0;
        long afternoonVolume = 0;
        for (IntradayData bar : bars) {
            LocalTime time = bar.getTradeTime();
            long vol = volume(bar);
            if (time.isBefore(LocalTime.of(10, 0))) {
                morningVolume += vol;
            } else if (time.isBefore(LocalTime.of(14, 0))) {
                midVolume += vol;
            } else {
                afternoonVolume += vol;
            }
        }
        long total = morningVolume + midVolume + afternoonVolume;
        if (total == 0) {
            return 50d;
        }
        double extremes = (morningVolume + afternoonVolume) / (double) total * 100;
        double avgVolume = bars.stream().mapToLong(this::volume).average().orElse(0);
        long spikes = bars.stream().filter(bar -> volume(bar) > avgVolume * 2.5).count();
        double spikeRate = bars.isEmpty() ? 0 : (double) spikes / bars.size() * 100;
        return Math.min(100, extremes + spikeRate * 20);
    }

    private double estimateIntradayDelivery(List<IntradayData> bars, PriceData yesterday) {
        if (bars.isEmpty() || yesterday == null) {
            return 50d;
        }
        IntradayData first = bars.get(0);
        IntradayData last = bars.get(bars.size() - 1);
        double priceChange = Math.abs(value(last.getClosePrice()) - value(first.getOpenPrice()));
        double totalRange = bars.stream()
                .mapToDouble(bar -> value(bar.getHighPrice()) - value(bar.getLowPrice()))
                .sum();
        double churn = priceChange == 0 ? 1 : totalRange / priceChange;
        double estimate = 60 - (churn * 5);
        return Math.max(10, Math.min(90, estimate));
    }

    private double calculatePriceRejectionProxy(List<IntradayData> bars) {
        if (bars.size() < 5) {
            return 50d;
        }
        double[] levels = identifyKeyLevels(bars);
        int rejections = 0;
        for (IntradayData bar : bars) {
            double high = value(bar.getHighPrice());
            double low = value(bar.getLowPrice());
            double close = value(bar.getClosePrice());
            for (double level : levels) {
                if (level == 0) {
                    continue;
                }
                boolean testedAbove = high >= level && close < level * 0.999;
                boolean testedBelow = low <= level && close > level * 1.001;
                if (testedAbove || testedBelow) {
                    rejections++;
                }
            }
        }
        double rate = (double) rejections / bars.size() * 100;
        return Math.min(100, rate * 10);
    }

    private double[] identifyKeyLevels(List<IntradayData> bars) {
        if (bars.isEmpty()) {
            return new double[0];
        }
        double open = value(bars.get(0).getOpenPrice());
        double current = value(bars.get(bars.size() - 1).getClosePrice());
        double round1 = Math.floor(current / 10d) * 10d;
        double round2 = Math.ceil(current / 10d) * 10d;
        return new double[]{open, round1, round2};
    }

    private double calculateVolumePriceCoherence(List<IntradayData> bars) {
        if (bars.size() < 2) {
            return 50d;
        }
        double avgVolume = bars.stream().mapToLong(this::volume).average().orElse(0);
        long incoherent = bars.stream().filter(bar -> {
            double move = Math.abs(priceChangePercent(bar));
            double volumeRatio = avgVolume == 0 ? 0 : volume(bar) / avgVolume;
            return (volumeRatio > 1.5 && move < 0.3) || (volumeRatio < 0.5 && move > 1);
        }).count();
        double rate = (double) incoherent / bars.size() * 100;
        return Math.min(100, rate * 3);
    }

    private double calculateMomentumExhaustion(List<IntradayData> bars) {
        if (bars.size() < 4) {
            return 50d;
        }
        int exhaustion = 0;
        for (int i = 3; i < bars.size(); i++) {
            IntradayData bar3 = bars.get(i - 3);
            IntradayData bar2 = bars.get(i - 2);
            IntradayData bar1 = bars.get(i - 1);
            IntradayData current = bars.get(i);
            boolean upMomentum = safeCompare(bar2.getClosePrice(), bar3.getClosePrice()) > 0
                    && safeCompare(bar1.getClosePrice(), bar2.getClosePrice()) > 0;
            boolean reversal = safeCompare(current.getClosePrice(), bar1.getClosePrice()) < 0;
            if (upMomentum && reversal) {
                exhaustion++;
            }
            boolean downMomentum = safeCompare(bar2.getClosePrice(), bar3.getClosePrice()) < 0
                    && safeCompare(bar1.getClosePrice(), bar2.getClosePrice()) < 0;
            boolean upReversal = safeCompare(current.getClosePrice(), bar1.getClosePrice()) > 0;
            if (downMomentum && upReversal) {
                exhaustion++;
            }
        }
        double rate = (double) exhaustion / bars.size() * 100;
        return Math.min(100, rate * 5);
    }

    private double calculateCompositeRetailScore(RetailIntensityMetrics metrics) {
        double score = 0;
        score += metrics.getVolatilityScore() * 0.25;
        score += metrics.getVolumeClusterScore() * 0.20;
        score += (100 - metrics.getEstimatedDeliveryPct()) * 0.20;
        score += metrics.getPriceRejectionScore() * 0.15;
        score += metrics.getVolumePriceCoherence() * 0.10;
        score += metrics.getMomentumExhaustion() * 0.10;
        return Math.max(0, Math.min(100, score));
    }

    private boolean evaluateAbsorptionQuality(MorningAnalysis morning, MidSessionAnalysis mid,
                                               double sessionDelta, boolean vwapReclaimed) {
        int score = 0;
        if (morning != null && morning.isPanicSellingDetected()) {
            score++;
        }
        if (mid != null && mid.isAbsorptionStarted()) {
            score++;
        }
        if (sessionDelta > 0) {
            score++;
        }
        if (vwapReclaimed) {
            score++;
        }
        return score >= 3;
    }

    private boolean evaluateSupplyExhaustion(List<IntradayData> bars, double sessionDelta) {
        if (bars.size() < 10) {
            return false;
        }
        int midpoint = bars.size() / 2;
        long firstHalfDown = bars.stream().limit(midpoint).filter(this::isDownBar).mapToLong(this::volume).sum();
        long secondHalfDown = bars.stream().skip(midpoint).filter(this::isDownBar).mapToLong(this::volume).sum();
        return secondHalfDown < firstHalfDown * 0.6 && sessionDelta > 0;
    }

    private double calculateWeakHandsScore(MorningAnalysis morning, MidSessionAnalysis mid, AfternoonAnalysis afternoon) {
        double score = 0;
        if (morning != null && morning.isPanicSellingDetected()) {
            score += 15;
        }
        if (mid != null && mid.isAbsorptionStarted()) {
            score += 15;
        }
        if (afternoon.getSessionCumulativeDelta() > 0) {
            score += 20;
        }
        double retailIntensity = afternoon.getRetailIntensity();
        if (retailIntensity < 40) {
            score += 15;
        } else if (retailIntensity < 60) {
            score += 10;
        }
        if (afternoon.isVwapReclaimed()) {
            score += 15;
        }
        if (afternoon.isGoodAbsorption()) {
            score += 10;
        }
        if (afternoon.isSupplyExhausted()) {
            score += 10;
        }
        return score;
    }

    private boolean generateBuySignal(AfternoonAnalysis analysis, double weakHandsScore) {
        return weakHandsScore >= 65
                && analysis.isVwapReclaimed()
                && analysis.getSessionCumulativeDelta() > 0
                && (analysis.isGoodAbsorption() || analysis.isSupplyExhausted());
    }

    private void calculateTradingLevels(AfternoonAnalysis analysis, PriceData yesterday, IntradayData latestBar) {
        double currentPrice = value(latestBar.getClosePrice());
        analysis.setEntryPrice(currentPrice);
        analysis.setTargetPrice(currentPrice * 1.03);
        double yesterdayHigh = value(yesterday.getHighPrice());
        double stop1 = yesterdayHigh * 0.99;
        double stop2 = currentPrice * 0.98;
        double stopLoss = Math.min(stop1, stop2);
        analysis.setStopLoss(stopLoss);
        double risk = currentPrice - stopLoss;
        double reward = analysis.getTargetPrice() - currentPrice;
        analysis.setRiskRewardRatio(risk <= 0 ? 0 : reward / risk);
        double stopPercent = currentPrice == 0 ? 0 : (risk / currentPrice) * 100;
        analysis.setPositionSizePercent(stopPercent == 0 ? 0 : 1.0 / stopPercent);
    }

    private double typicalPrice(IntradayData bar) {
        double high = value(bar.getHighPrice());
        double low = value(bar.getLowPrice());
        double close = value(bar.getClosePrice());
        return (high + low + close) / 3d;
    }

    private double priceChange(IntradayData bar) {
        return value(bar.getClosePrice()) - value(bar.getOpenPrice());
    }

    private double priceChangePercent(IntradayData bar) {
        double open = value(bar.getOpenPrice());
        if (open == 0) {
            return 0;
        }
        return (value(bar.getClosePrice()) - open) / open * 100;
    }

    private long volume(IntradayData bar) {
        return bar.getVolume() == null ? 0 : bar.getVolume();
    }

    private boolean isDownBar(IntradayData bar) {
        return safeCompare(bar.getClosePrice(), bar.getOpenPrice()) < 0;
    }

    private double computeGap(BigDecimal open, BigDecimal close) {
        if (open == null || close == null || close.doubleValue() == 0) {
            return 0;
        }
        return open.subtract(close).divide(close, 4, RoundingMode.HALF_UP).doubleValue() * 100;
    }

    private double value(BigDecimal input) {
        return input == null ? 0 : input.doubleValue();
    }

    private int safeCompare(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        return a.compareTo(b);
    }

    @Data
    public static class MorningAnalysis {
        private String symbol;
        private LocalDate analysisDate;
        private double gapPercentage;
        private double morningDelta;
        private long morningVolume;
        private int downBarsCount;
        private double avgTradeSize;
        private boolean panicSellingDetected;
    }

    @Data
    public static class MidSessionAnalysis {
        private String symbol;
        private LocalDate analysisDate;
        private double midSessionDelta;
        private boolean priceRecovering;
        private double currentVwap;
        private boolean aboveVwap;
        private boolean absorptionStarted;
    }

    @Data
    public static class AfternoonAnalysis {
        private String symbol;
        private LocalDate analysisDate;
        private LocalTime analysisTime;
        private double currentPrice;
        private double sessionCumulativeDelta;
        private double retailIntensity;
        private boolean vwapReclaimed;
        private boolean goodAbsorption;
        private boolean supplyExhausted;
        private double weakHandsScore;
        private boolean buySignal;
        private double entryPrice;
        private double targetPrice;
        private double stopLoss;
        private double riskRewardRatio;
        private double positionSizePercent;
    }

    @Data
    public static class RetailIntensityMetrics {
        private double volatilityScore;
        private double volumeClusterScore;
        private double estimatedDeliveryPct;
        private double priceRejectionScore;
        private double volumePriceCoherence;
        private double momentumExhaustion;
        private double retailIntensityScore;
    }
}
