package com.stockanalyzer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnrichedTechnicalIndicatorDTO extends TechnicalIndicatorDTO {
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private Map<String, Double> supportLevels;
    private Map<String, Double> resistanceLevels;
    private List<String> earlyBirdRecommendations;
    private List<TechnicalIndicatorDTO> historicalIndicators;

    @Builder(builderMethodName = "enrichedBuilder")
    public EnrichedTechnicalIndicatorDTO(String symbol, LocalDate calculationDate, LocalDateTime createdAt, Double rsi14, Double ema9, Double ema21, Double sma20, Double atr14, Double vwap, Double volumeRatio, Double priceStrength, Double volumeStrength, Double deliveryStrength, Double macd, Double macdSignal, Double macdHistogram, Double bollingerUpper, Double bollingerLower, Double bollingerWidth, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, Map<String, Double> supportLevels, Map<String, Double> resistanceLevels, List<String> earlyBirdRecommendations, List<TechnicalIndicatorDTO> historicalIndicators) {
        super(symbol, calculationDate, createdAt, rsi14, ema9, ema21, sma20, atr14, vwap, volumeRatio, priceStrength, volumeStrength, deliveryStrength, macd, macdSignal, macdHistogram, bollingerUpper, bollingerLower, bollingerWidth);
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.supportLevels = supportLevels;
        this.resistanceLevels = resistanceLevels;
        this.earlyBirdRecommendations = earlyBirdRecommendations;
        this.historicalIndicators = historicalIndicators;
    }
}
