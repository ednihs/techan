package com.stockanalyzer.repository;

import com.stockanalyzer.entity.PriceData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceDataRepository extends JpaRepository<PriceData, Long> {

    Optional<PriceData> findBySymbolAndTradeDate(String symbol, LocalDate tradeDate);

    List<PriceData> findByTradeDateAndVolumeGreaterThanAndValueTradedGreaterThan(LocalDate tradeDate,
                                                                                 Long minVolume,
                                                                                 Long minValueTraded);

    List<PriceData> findTop20BySymbolAndTradeDateLessThanOrderByTradeDateDesc(String symbol, LocalDate tradeDate);

    List<PriceData> findTop10BySymbolAndTradeDateLessThanEqualOrderByTradeDateDesc(String symbol, LocalDate tradeDate);

    boolean existsByTradeDate(LocalDate tradeDate);

    @Query("SELECT p FROM PriceData p WHERE p.symbol = :symbol AND p.tradeDate BETWEEN :startDate AND :endDate ORDER BY p.tradeDate")
    List<PriceData> findHistoricalData(@Param("symbol") String symbol,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);
}
