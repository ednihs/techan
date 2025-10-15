package com.stockanalyzer.repository;

import com.stockanalyzer.entity.TechnicalIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface TechnicalIndicatorRepository extends JpaRepository<TechnicalIndicator, Long> {
    TechnicalIndicator findBySymbolAndCalculationDate(String symbol, LocalDate calculationDate);
}
