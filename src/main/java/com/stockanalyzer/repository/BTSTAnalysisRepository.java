package com.stockanalyzer.repository;

import com.stockanalyzer.entity.BTSTAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BTSTAnalysisRepository extends JpaRepository<BTSTAnalysis, Long> {

    List<BTSTAnalysis> findByAnalysisDateAndRecommendationOrderByConfidenceScoreDesc(LocalDate analysisDate,
                                                                                     String recommendation);

    Optional<BTSTAnalysis> findBySymbolAndAnalysisDate(String symbol, LocalDate analysisDate);
}
