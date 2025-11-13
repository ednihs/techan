package com.stockanalyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "intraday_data",
        indexes = {
                @Index(name = "idx_intraday_symbol_date_time", columnList = "symbol, trade_date, trade_time"),
                @Index(name = "idx_intraday_trade_date", columnList = "trade_date")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntradayData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "trade_time", nullable = false)
    private LocalTime tradeTime;

    @Column(name = "open_price", precision = 12, scale = 2)
    private BigDecimal openPrice;

    @Column(name = "high_price", precision = 12, scale = 2)
    private BigDecimal highPrice;

    @Column(name = "low_price", precision = 12, scale = 2)
    private BigDecimal lowPrice;

    @Column(name = "close_price", precision = 12, scale = 2)
    private BigDecimal closePrice;

    @Column
    private Long volume;

    @Column(precision = 12, scale = 2)
    private BigDecimal bid;

    @Column(precision = 12, scale = 2)
    private BigDecimal ask;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
