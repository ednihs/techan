package com.stockanalyzer.repository;

import com.stockanalyzer.entity.CrudeOHLCVData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing crude oil OHLCV data.
 */
public interface CrudeOHLCVDataRepository extends JpaRepository<CrudeOHLCVData, Long> {

    /**
     * Find OHLCV data by symbol, timeframe, and timestamp range
     */
    List<CrudeOHLCVData> findBySymbolAndTimeframeAndTimestampBetweenOrderByTimestampAsc(
            String symbol,
            CrudeOHLCVData.Timeframe timeframe,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Find by symbol and timeframe, ordered by timestamp descending
     */
    List<CrudeOHLCVData> findBySymbolAndTimeframeOrderByTimestampDesc(
            String symbol,
            CrudeOHLCVData.Timeframe timeframe
    );

    /**
     * Find top N records by symbol and timeframe
     */
    List<CrudeOHLCVData> findTop200BySymbolAndTimeframeOrderByTimestampDesc(
            String symbol,
            CrudeOHLCVData.Timeframe timeframe
    );

    /**
     * Check if data exists for a specific candle
     */
    boolean existsBySymbolAndTimeframeAndTimestamp(
            String symbol,
            CrudeOHLCVData.Timeframe timeframe,
            LocalDateTime timestamp
    );

    /**
     * Find unique data point by symbol, timeframe, and timestamp
     */
    Optional<CrudeOHLCVData> findBySymbolAndTimeframeAndTimestamp(
            String symbol,
            CrudeOHLCVData.Timeframe timeframe,
            LocalDateTime timestamp
    );

    /**
     * Count records for a given timeframe and date range
     */
    @Query("SELECT COUNT(c) FROM CrudeOHLCVData c WHERE c.symbol = :symbol AND c.timeframe = :timeframe " +
            "AND c.timestamp BETWEEN :startDate AND :endDate")
    long countBySymbolAndTimeframeAndDateRange(
            @Param("symbol") String symbol,
            @Param("timeframe") CrudeOHLCVData.Timeframe timeframe,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find latest timestamp for a given symbol and timeframe
     */
    @Query("SELECT MAX(c.timestamp) FROM CrudeOHLCVData c WHERE c.symbol = :symbol AND c.timeframe = :timeframe")
    Optional<LocalDateTime> findLatestTimestampBySymbolAndTimeframe(
            @Param("symbol") String symbol,
            @Param("timeframe") CrudeOHLCVData.Timeframe timeframe
    );

    /**
     * Find OHLCV data by multiple timeframes
     */
    @Query("SELECT c FROM CrudeOHLCVData c WHERE c.symbol = :symbol AND c.timeframe IN :timeframes " +
            "AND c.timestamp BETWEEN :startDate AND :endDate ORDER BY c.timeframe, c.timestamp ASC")
    List<CrudeOHLCVData> findBySymbolAndTimeframesAndDateRange(
            @Param("symbol") String symbol,
            @Param("timeframes") List<CrudeOHLCVData.Timeframe> timeframes,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}

