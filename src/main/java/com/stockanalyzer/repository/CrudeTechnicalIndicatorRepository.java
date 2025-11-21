package com.stockanalyzer.repository;

import com.stockanalyzer.entity.CrudeOHLCVData;
import com.stockanalyzer.entity.CrudeTechnicalIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing crude oil technical indicators.
 */
public interface CrudeTechnicalIndicatorRepository extends JpaRepository<CrudeTechnicalIndicator, Long> {

    /**
     * Find indicator by OHLCV ID
     */
    Optional<CrudeTechnicalIndicator> findByOhlcvId(Long ohlcvId);

    /**
     * Check if indicator exists for OHLCV ID
     */
    boolean existsByOhlcvId(Long ohlcvId);

    /**
     * Find indicators by timeframe and timestamp range
     */
    List<CrudeTechnicalIndicator> findByTimeframeAndTimestampBetweenOrderByTimestampAsc(
            CrudeOHLCVData.Timeframe timeframe,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Find indicators by multiple timeframes and date range
     */
    @Query("SELECT i FROM CrudeTechnicalIndicator i WHERE i.timeframe IN :timeframes " +
            "AND i.timestamp BETWEEN :startDate AND :endDate ORDER BY i.timeframe, i.timestamp ASC")
    List<CrudeTechnicalIndicator> findByTimeframesAndDateRange(
            @Param("timeframes") List<CrudeOHLCVData.Timeframe> timeframes,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find indicators by data quality flag
     */
    List<CrudeTechnicalIndicator> findByDataQualityFlagOrderByTimestampDesc(
            CrudeTechnicalIndicator.DataQualityFlag dataQualityFlag
    );

    /**
     * Find indicators by data quality flags
     */
    @Query("SELECT i FROM CrudeTechnicalIndicator i WHERE i.dataQualityFlag IN :flags " +
            "AND i.timestamp BETWEEN :startDate AND :endDate ORDER BY i.timestamp DESC")
    List<CrudeTechnicalIndicator> findByDataQualityFlagsAndDateRange(
            @Param("flags") List<CrudeTechnicalIndicator.DataQualityFlag> flags,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find latest indicators by timeframe with limit
     */
    @Query("SELECT i FROM CrudeTechnicalIndicator i WHERE i.timeframe = :timeframe " +
            "ORDER BY i.timestamp DESC LIMIT :limit")
    List<CrudeTechnicalIndicator> findLatestByTimeframe(
            @Param("timeframe") CrudeOHLCVData.Timeframe timeframe,
            @Param("limit") int limit
    );

    /**
     * Count indicators by timeframe and date range
     */
    @Query("SELECT COUNT(i) FROM CrudeTechnicalIndicator i WHERE i.timeframe = :timeframe " +
            "AND i.timestamp BETWEEN :startDate AND :endDate")
    long countByTimeframeAndDateRange(
            @Param("timeframe") CrudeOHLCVData.Timeframe timeframe,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find indicators with joined OHLCV data for consolidated export
     */
    @Query("SELECT i FROM CrudeTechnicalIndicator i JOIN CrudeOHLCVData o ON i.ohlcvId = o.id " +
            "WHERE i.timeframe IN :timeframes AND i.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY i.timeframe, i.timestamp ASC")
    List<CrudeTechnicalIndicator> findWithOHLCVByTimeframesAndDateRange(
            @Param("timeframes") List<CrudeOHLCVData.Timeframe> timeframes,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Delete indicators older than specified timestamp
     */
    void deleteByTimestampBefore(LocalDateTime timestamp);
}

