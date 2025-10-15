package com.stockanalyzer.service;

import com.stockanalyzer.entity.BTSTAnalysis;
import com.stockanalyzer.entity.PriceData;
import com.stockanalyzer.repository.PriceDataRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskAssessmentService {

    private final PriceDataRepository priceDataRepository;

    public LiquidityRisk calculateLiquidityRisk(String symbol, LocalDate date) {
        try {
            PriceData current = priceDataRepository.findBySymbolAndTradeDate(symbol, date).orElse(null);
            List<PriceData> history = priceDataRepository.findTop10BySymbolAndTradeDateLessThanEqualOrderByTradeDateDesc(symbol, date);
            if (current == null || history.size() < 5) {
                return new LiquidityRisk("HIGH", "Insufficient data for liquidity assessment");
            }
            double avgVolume = history.stream().mapToLong(PriceData::getVolume).average().orElse(0);
            double avgTurnover = history.stream().mapToLong(pd -> pd.getValueTraded() == null ? 0L : pd.getValueTraded()).average().orElse(0);
            BigDecimal range = current.getHighPrice().subtract(current.getLowPrice());
            BigDecimal midPrice = current.getHighPrice().add(current.getLowPrice()).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
            double spreadPercentage = midPrice.doubleValue() == 0 ? 0 : range.divide(midPrice, 4, RoundingMode.HALF_UP).doubleValue() * 100;
            double turnoverRatio = avgTurnover == 0 ? 0 : current.getValueTraded() / avgTurnover;
            int score = 0;
            if (current.getVolume() > avgVolume * 1.5) score += 3;
            else if (current.getVolume() > avgVolume) score += 2;
            else if (current.getVolume() > avgVolume * 0.5) score += 1;

            if (current.getValueTraded() != null && current.getValueTraded() > 100_000_000L) score += 3;
            else if (current.getValueTraded() != null && current.getValueTraded() > 50_000_000L) score += 2;
            else if (current.getValueTraded() != null && current.getValueTraded() > 20_000_000L) score += 1;

            if (spreadPercentage < 0.5) score += 3;
            else if (spreadPercentage < 1.0) score += 2;
            else if (spreadPercentage < 2.0) score += 1;

            if (turnoverRatio > 0.8 && turnoverRatio < 2.0) score += 2;
            else if (turnoverRatio > 0.5) score += 1;

            String level = score >= 9 ? "LOW" : score >= 6 ? "MEDIUM" : "HIGH";
            StringBuilder factors = new StringBuilder();
            if (current.getVolume() > avgVolume) {
                factors.append("Healthy volume, ");
            }
            if (spreadPercentage < 1.0) {
                factors.append("Tight spread, ");
            }
            if (turnoverRatio > 1.0) {
                factors.append("Turnover above average");
            }
            return new LiquidityRisk(level, factors.toString().trim());
        } catch (Exception ex) {
            log.error("Error computing liquidity risk for {}: {}", symbol, ex.getMessage());
            return new LiquidityRisk("HIGH", "Error in liquidity calculation");
        }
    }

    public GapRisk calculateGapRisk(String symbol, LocalDate date, BTSTAnalysis analysis) {
        try {
            PriceData current = priceDataRepository.findBySymbolAndTradeDate(symbol, date).orElse(null);
            List<PriceData> history = priceDataRepository.findTop10BySymbolAndTradeDateLessThanEqualOrderByTradeDateDesc(symbol, date.minusDays(1));
            if (current == null || history.size() < 5) {
                return new GapRisk("MEDIUM", "Insufficient historical gaps");
            }
            int gapCount = 0;
            double totalGap = 0;
            PriceData prev = null;
            for (PriceData day : history) {
                if (prev != null) {
                    double gap = day.getOpenPrice().subtract(prev.getClosePrice())
                            .divide(prev.getClosePrice(), 4, RoundingMode.HALF_UP).doubleValue() * 100;
                    if (Math.abs(gap) > 0.5) {
                        gapCount++;
                        totalGap += Math.abs(gap);
                    }
                }
                prev = day;
            }
            double avgGap = gapCount == 0 ? 0 : totalGap / gapCount;
            double range = current.getHighPrice().subtract(current.getLowPrice()).divide(current.getClosePrice(), 4, RoundingMode.HALF_UP).doubleValue() * 100;
            double score = 0;
            if (gapCount > 5) score += 3;
            else if (gapCount > 3) score += 2;
            else if (gapCount > 1) score += 1;

            if (avgGap > 3) score += 3;
            else if (avgGap > 1.5) score += 2;
            else if (avgGap > 0.5) score += 1;

            if (range > 4) score += 2;
            else if (range > 2) score += 1;

            if (analysis != null && analysis.getGapPercentage() != null && Math.abs(analysis.getGapPercentage()) > 0.5) {
                score += 2;
            }
            if (analysis != null && Boolean.FALSE.equals(analysis.getShowsAbsorption())) {
                score += 1;
            }
            String level = score <= 3 ? "LOW" : score <= 6 ? "MEDIUM" : "HIGH";
            StringBuilder factors = new StringBuilder();
            factors.append("Gap count: ").append(gapCount).append(", avg size: ")
                    .append(String.format("%.2f", avgGap)).append("% ");
            factors.append("Range: ").append(String.format("%.2f", range)).append("%");
            return new GapRisk(level, factors.toString());
        } catch (Exception ex) {
            log.error("Error calculating gap risk for {}: {}", symbol, ex.getMessage());
            return new GapRisk("MEDIUM", "Error in gap risk calculation");
        }
    }

    @Data
    @AllArgsConstructor
    public static class LiquidityRisk {
        private String level;
        private String factors;
    }

    @Data
    @AllArgsConstructor
    public static class GapRisk {
        private String level;
        private String factors;
    }
}
