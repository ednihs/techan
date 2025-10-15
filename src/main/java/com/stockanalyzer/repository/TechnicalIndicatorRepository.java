package com.stockanalyzer.repository;

import com.stockanalyzer.entity.TechnicalIndicator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TechnicalIndicatorRepository extends JpaRepository<TechnicalIndicator, Long> {

    Optional<TechnicalIndicator> findBySymbolAndCalculationDate(String symbol, LocalDate calculationDate);
}
