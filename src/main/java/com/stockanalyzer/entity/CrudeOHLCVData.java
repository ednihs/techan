package com.stockanalyzer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing OHLCV (Open, High, Low, Close, Volume) candle data for crude oil.
 * Stores raw price and volume data fetched from market data providers.
 */
@Entity
@Table(name = "crude_ohlcv_data",
        uniqueConstraints = @UniqueConstraint(name = "unique_candle", columnNames = {"symbol", "timeframe", "timestamp"}),
        indexes = {
                @Index(name = "idx_timeframe_timestamp", columnList = "timeframe, timestamp DESC"),
                @Index(name = "idx_symbol_timeframe", columnList = "symbol, timeframe")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CrudeOHLCVData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Timeframe timeframe;

    @Builder.Default
    @Column(nullable = false, length = 10)
    private String symbol = "BRN"; // Default to Brent Crude

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal open;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal high;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal low;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal close;

    @Column(nullable = false)
    private Long volume;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Timeframe {
        @SuppressWarnings("unused")
        FOUR_H("4H"),
        @SuppressWarnings("unused")
        ONE_H("1H"),
        @SuppressWarnings("unused")
        FIFTEEN_M("15M");

        private final String value;

        Timeframe(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Timeframe fromValue(String value) {
            for (Timeframe tf : values()) {
                if (tf.value.equals(value)) {
                    return tf;
                }
            }
            throw new IllegalArgumentException("Unknown timeframe: " + value);
        }
    }
}

