package com.stockanalyzer.repository;

import com.stockanalyzer.entity.PriceData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceDataRepository extends JpaRepository<PriceData, Long> {

    Optional<PriceData>
    findBySymbolAndTradeDate(String symbol, LocalDate tradeDate);

    Optional<PriceData> findTopBySymbolAndTradeDateBetweenOrderByClosePriceDesc(String symbol, LocalDate startDate, LocalDate endDate);
    Optional<PriceData> findTopBySymbolAndTradeDateBetweenOrderByClosePriceAsc(String symbol, LocalDate startDate, LocalDate endDate);
    List<PriceData> findTop5BySymbolAndTradeDateLessThanEqualOrderByTradeDateDesc(String symbol, LocalDate date);

    @Query("SELECT MAX(p.tradeDate) FROM PriceData p")
    Optional<LocalDate> findLatestTradeDate();

    List<PriceData> findBySymbolInAndTradeDate(List<String> symbols, LocalDate tradeDate);

    List<PriceData> findTop20BySymbolAndTradeDateLessThanOrderByTradeDateDesc(String symbol, LocalDate tradeDate);

    List<PriceData> findByTradeDate(LocalDate tradeDate);

    boolean existsByTradeDate(LocalDate tradeDate);

    @Query("SELECT p FROM PriceData p WHERE p.symbol = :symbol AND p.tradeDate >= :startDate AND p.tradeDate <= :endDate ORDER BY p.tradeDate ASC")
    List<PriceData> findHistoricalData(@Param("symbol") String symbol,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT p.tradeDate FROM PriceData p WHERE p.symbol = :symbol AND p.tradeDate <= :endDate ORDER BY p.tradeDate DESC")
    List<LocalDate> findLastNTradeDates(@Param("symbol") String symbol, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT DISTINCT p.symbol FROM PriceData p")
    List<String> findDistinctSymbols();

    @Query("SELECT p.symbol FROM PriceData p WHERE p.tradeDate BETWEEN :startDate AND :endDate GROUP BY p.symbol HAVING AVG(p.valueTraded) >= :minValue")
    List<String> findSymbolsByAverageValueTraded(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("minValue") double minValue
    );

    @Query("SELECT p FROM PriceData p WHERE p.tradeDate = :tradeDate AND p.symbol IN " +
           "(SELECT p2.symbol FROM PriceData p2 WHERE p2.tradeDate BETWEEN :startDate AND :endDate GROUP BY p2.symbol HAVING AVG(p2.valueTraded) >= :minValue)")
    List<PriceData> findByTradeDateWithAverageValueTradedFilter(
            @Param("tradeDate") LocalDate tradeDate,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("minValue") double minValue
    );

    List<PriceData> findBySymbolAndTradeDateBetween(String symbol, LocalDate startDate, LocalDate endDate);
    List<PriceData> findTop5BySymbolOrderByTradeDateDesc(String symbol);

    @Query("SELECT p.closePrice FROM PriceData p WHERE p.symbol = :symbol ORDER BY p.tradeDate DESC")
    List<Double> findRecentClosePrices(@Param("symbol") String symbol, Pageable pageable);

    @Query("SELECT p FROM PriceData p WHERE p.symbol = :symbol AND p.volume > :minValue")
    List<PriceData> findBySymbolWithVolumeGreaterThan(
            @Param("symbol") String symbol,
            @Param("minValue") double minValue
    );

    @Query("SELECT DISTINCT p.symbol FROM PriceData p WHERE p.tradeDate = :date")
    List<String> findDistinctSymbolsByTradeDate(@Param("date") LocalDate date);

    List<PriceData> findBySymbolAndTradeDateBeforeOrderByTradeDateDesc(String symbol, LocalDate date, Pageable pageable);
}
