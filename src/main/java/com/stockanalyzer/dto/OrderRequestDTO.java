package com.stockanalyzer.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderRequestDTO {
    private String symbol;
    private String exchange;
    private String orderType; // MARKET, LIMIT
    private String transactionType; // BUY, SELL
    private int quantity;
    private BigDecimal price;
    private String productType; // INTRADAY, DELIVERY
}
