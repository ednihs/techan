package com.stockanalyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_data", 
       indexes = {
           @Index(name = "idx_symbol_date", columnList = "symbol, trade_date"),
           @Index(name = "idx_trade_date", columnList = "trade_date")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String symbol;
    
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    
    @Column(name = "open_price")
    private Double open;
    
    @Column(name = "high_price")
    private Double high;
    
    @Column(name = "low_price")
    private Double low;
    
    @Column(name = "close_price")
    private Double close;
    
    @Column(name = "prev_close")
    private Double prevClose;
    
    private Long volume;

    @Column(name = "value_traded")
    private Long value;

    @Column(name = "no_of_trades")
    private Integer noOfTrades;
    
    private Double deliveryPercentage;
    
    @CreatedDate
    private LocalDateTime createdAt;
}
