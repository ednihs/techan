package com.stockanalyzer.service;

import com.stockanalyzer.entity.BTSTAnalysis;
import com.stockanalyzer.entity.PriceData;
import com.stockanalyzer.repository.PriceDataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentServiceTest {

    @Mock
    private PriceDataRepository priceDataRepository;

    @InjectMocks
    private RiskAssessmentService riskAssessmentService;

    @Test
    void returnsHighRiskWhenInsufficientData() {
        when(priceDataRepository.findBySymbolAndTradeDate(any(), any())).thenReturn(Optional.empty());
        var risk = riskAssessmentService.calculateLiquidityRisk("ABC", LocalDate.now());
        assertThat(risk.getLevel()).isEqualTo("HIGH");
    }

    @Test
    void calculatesGapRiskWithHistory() {
        PriceData current = PriceData.builder()
                .symbol("ABC")
                .tradeDate(LocalDate.now())
                .openPrice(BigDecimal.valueOf(100))
                .highPrice(BigDecimal.valueOf(110))
                .lowPrice(BigDecimal.valueOf(95))
                .closePrice(BigDecimal.valueOf(105))
                .volume(1_000_000L)
                .valueTraded(50_000_000L)
                .build();
        when(priceDataRepository.findBySymbolAndTradeDate(any(), any())).thenReturn(Optional.of(current));
        List<PriceData> history = List.of(
                PriceData.builder().openPrice(BigDecimal.valueOf(99.6)).closePrice(BigDecimal.valueOf(100)).build(),
                PriceData.builder().openPrice(BigDecimal.valueOf(99.7)).closePrice(BigDecimal.valueOf(99.6)).build(),
                PriceData.builder().openPrice(BigDecimal.valueOf(99.8)).closePrice(BigDecimal.valueOf(99.7)).build(),
                PriceData.builder().openPrice(BigDecimal.valueOf(99.9)).closePrice(BigDecimal.valueOf(99.8)).build(),
                PriceData.builder().openPrice(BigDecimal.valueOf(99.0)).closePrice(BigDecimal.valueOf(99.9)).build()
        );
        when(priceDataRepository.findTop20BySymbolAndTradeDateLessThanOrderByTradeDateDesc(any(), any()))
                .thenReturn(history);
        var risk = riskAssessmentService.calculateGapRisk("ABC", LocalDate.now(), null);
        assertThat(risk.getLevel()).isEqualTo("LOW");
    }

    @Test
    void calculatesGapRiskWithBTSTAnalysis() {
        PriceData current = PriceData.builder()
                .symbol("ABC")
                .tradeDate(LocalDate.now())
                .openPrice(BigDecimal.valueOf(120))
                .highPrice(BigDecimal.valueOf(125))
                .lowPrice(BigDecimal.valueOf(118))
                .closePrice(BigDecimal.valueOf(122))
                .build();

        List<PriceData> history = List.of(
                PriceData.builder().openPrice(BigDecimal.valueOf(109)).closePrice(BigDecimal.valueOf(110)).build(),
                PriceData.builder().openPrice(BigDecimal.valueOf(113)).closePrice(BigDecimal.valueOf(115)).build(),
                PriceData.builder().openPrice(BigDecimal.valueOf(114)).closePrice(BigDecimal.valueOf(118)).build(),
                PriceData.builder().openPrice(BigDecimal.valueOf(119)).closePrice(BigDecimal.valueOf(120)).build(),
                PriceData.builder().openPrice(BigDecimal.valueOf(121)).closePrice(BigDecimal.valueOf(122)).build()
        );

        when(priceDataRepository.findBySymbolAndTradeDate(any(), any())).thenReturn(Optional.of(current));
        when(priceDataRepository.findTop20BySymbolAndTradeDateLessThanOrderByTradeDateDesc(any(), any()))
                .thenReturn(history);

        BTSTAnalysis analysis = BTSTAnalysis.builder()
                .gapPercentage(1.0)
                .showsAbsorption(false)
                .build();

        var risk = riskAssessmentService.calculateGapRisk("ABC", LocalDate.now(), analysis);
        assertThat(risk.getLevel()).isEqualTo("HIGH");
    }
}
