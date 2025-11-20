package com.stockanalyzer.repository;

import com.stockanalyzer.entity.TechnicalIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TechnicalIndicatorRepository extends JpaRepository<TechnicalIndicator, Long> {

    Optional<TechnicalIndicator> findBySymbolAndCalculationDate(String symbol, LocalDate calculationDate);
    List<TechnicalIndicator> findBySymbolAndCalculationDateInOrderByCalculationDateDesc(String symbol, List<LocalDate> dates);
    List<TechnicalIndicator> findBySymbolInAndCalculationDateInOrderByCalculationDateDesc(List<String> symbols, List<LocalDate> dates);

    @Query("SELECT ti " +
            "FROM TechnicalIndicator ti JOIN PriceData pd " +
            "ON ti.symbol = pd.symbol AND ti.calculationDate = pd.tradeDate " +
           "WHERE ti.calculationDate = :date AND ti.symbol IN :symbols")
     List<TechnicalIndicator> findEnrichedTechnicalIndicators(@Param("date") LocalDate date, @Param("symbols") List<String> symbols);

    List<TechnicalIndicator> findBySymbolAndCalculationDateBetweenOrderByCalculationDateDesc(String symbol, LocalDate startDate, LocalDate endDate);

    List<TechnicalIndicator> findTop5BySymbolAndCalculationDateLessThanEqualOrderByCalculationDateDesc(String symbol, LocalDate date);
    
    List<TechnicalIndicator> findTop10BySymbolAndCalculationDateLessThanEqualOrderByCalculationDateDesc(String symbol, LocalDate date);
    
    @Query("SELECT MAX(ti.calculationDate) FROM TechnicalIndicator ti WHERE ti.symbol = :symbol")
    Optional<LocalDate> findLatestCalculationDateBySymbol(@Param("symbol") String symbol);
}
