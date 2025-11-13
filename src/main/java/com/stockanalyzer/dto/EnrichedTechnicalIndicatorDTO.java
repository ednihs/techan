package com.stockanalyzer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvIgnore;
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
    @CsvBindByName(column = "Open")
    private BigDecimal open;
    @CsvBindByName(column = "High")
    private BigDecimal high;
    @CsvBindByName(column = "Low")
    private BigDecimal low;
    @CsvBindByName(column = "Close")
    private BigDecimal close;
    @CsvIgnore
    private Map<String, Double> supportLevels;
    @CsvIgnore
    private Map<String, Double> resistanceLevels;
    @CsvIgnore
    private List<String> earlyBirdRecommendations;
    @CsvIgnore
    private List<TechnicalIndicatorDTO> historicalIndicators;

    @Builder(builderMethodName = "enrichedBuilder")
    public EnrichedTechnicalIndicatorDTO(String symbol, LocalDate calculationDate, LocalDateTime createdAt,
                                         Double rsi14, Double ema9, Double ema21, Double sma20, Double atr14,
                                         Double vwap, Double volumeRatio, Double priceStrength, Double volumeStrength,
                                         Double deliveryStrength, Double macd, Double macdSignal, Double macdHistogram,
                                         Double bollingerUpper, Double bollingerLower, Double bollingerWidth,
                                         Float week52High, Float week52Low, Integer daysSince52wHigh,
                                         Float intradayFadePct, Float closeVelocity5d, Float prevDayOpen,
                                         Float prevDayHigh, Float prevDayLow, Float prevDayClose,
                                         Float rsi14PrevDay, Float macdHistogramPrevDay, Float pivotPoint,
                                         Float resistance1, Float resistance2, Float support1, Float support2,
                                         Float pctFromPivot, Float pctToResistance1, Float pctToResistance2,
                                         Float pctToSupport1, Float pctToSupport2, String pricePositionStage,
                                         String momentumDirection, String dataCompleteness,
                                         Float deliveryStrength5dAvg, String deliveryStrengthTrend,
                                         BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
                                         Map<String, Double> supportLevels, Map<String, Double> resistanceLevels,
                                         List<String> earlyBirdRecommendations, List<TechnicalIndicatorDTO> historicalIndicators) {
        super(symbol, calculationDate, createdAt, rsi14, ema9, ema21, sma20, atr14, vwap, volumeRatio,
              priceStrength, volumeStrength, deliveryStrength, macd, macdSignal, macdHistogram,
              bollingerUpper, bollingerLower, bollingerWidth, week52High, week52Low, daysSince52wHigh,
              intradayFadePct, closeVelocity5d, prevDayOpen, prevDayHigh, prevDayLow, prevDayClose,
              rsi14PrevDay, macdHistogramPrevDay, pivotPoint, resistance1, resistance2, support1, support2,
              pctFromPivot, pctToResistance1, pctToResistance2, pctToSupport1, pctToSupport2,
              pricePositionStage, momentumDirection, dataCompleteness,
              deliveryStrength5dAvg, deliveryStrengthTrend);
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
