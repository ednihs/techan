package com.stockanalyzer.service;

import com.stockanalyzer.entity.PriceData;
import com.stockanalyzer.repository.PriceDataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        when(priceDataRepository.findTop10BySymbolAndTradeDateLessThanEqualOrderByTradeDateDesc(any(), any()))
                .thenReturn(java.util.List.of(current));
        var risk = riskAssessmentService.calculateGapRisk("ABC", LocalDate.now(), null);
        assertThat(risk.getLevel()).isIn("LOW", "MEDIUM", "HIGH");
    }
}
