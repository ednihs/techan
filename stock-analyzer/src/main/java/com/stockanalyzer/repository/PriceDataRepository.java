package com.stockanalyzer.repository;

import com.stockanalyzer.entity.PriceData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PriceDataRepository extends JpaRepository<PriceData, Long> {
    
    PriceData findBySymbolAndTradeDate(String symbol, LocalDate tradeDate);
    
    List<PriceData> findBySymbolAndTradeDateBetweenOrderByTradeDate(
            String symbol, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT DISTINCT p.symbol FROM PriceData p WHERE p.tradeDate = :date")
    List<String> findActiveSymbols(@Param("date") LocalDate date);
    
    @Query("SELECT p FROM PriceData p WHERE p.trade_date = :date " +
           "AND p.volume > :minVolume AND p.value_traded > :minValue " +
           "ORDER BY p.volume DESC")
    List<PriceData> findByTradeDateAndVolumeGreaterThanAndValueTradedGreaterThan(
            @Param("date") LocalDate date, 
            @Param("minVolume") Long minVolume, 
            @Param("minValue") Long minValue);
    
    @Query(value = "SELECT * FROM price_data p WHERE p.symbol = :symbol " +
           "AND p.trade_date < :date ORDER BY p.trade_date DESC LIMIT :limit", nativeQuery = true)
    List<PriceData> findTopNBySymbolAndTradeDateLessThanOrderByTradeDateDesc(
            @Param("symbol") String symbol, 
            @Param("date") LocalDate date, 
            @Param("limit") int limit);
    
    @Query(value = "SELECT * FROM price_data p WHERE p.symbol = :symbol " +
           "AND p.trade_date <= :date ORDER BY p.trade_date DESC LIMIT :limit", nativeQuery = true)
    List<PriceData> findTopNBySymbolAndTradeDateLessThanEqualOrderByTradeDateDesc(
            @Param("symbol") String symbol, 
            @Param("date") LocalDate date, 
            @Param("limit") int limit);
    
    // Performance optimized queries with MySQL specific hints
    @Query(value = "SELECT * FROM price_data USE INDEX (idx_symbol_date_desc) " +
                   "WHERE symbol = :symbol AND trade_date BETWEEN :startDate AND :endDate " +
                   "ORDER BY trade_date DESC", nativeQuery = true)
    List<PriceData> findHistoricalDataOptimized(
            @Param("symbol") String symbol,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
