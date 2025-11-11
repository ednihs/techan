package com.stockanalyzer.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class HoldingDTO {
    private String symbol;
    private String exchange;
    private int quantity;
    private BigDecimal averagePrice;
    private BigDecimal lastTradedPrice;
    private BigDecimal pnl;
    private BigDecimal dayPnl;
}
