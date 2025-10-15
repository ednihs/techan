package com.stockanalyzer.repository;

import com.stockanalyzer.entity.BTSTAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BTSTAnalysisRepository extends JpaRepository<BTSTAnalysis, Long> {
    BTSTAnalysis findBySymbolAndAnalysisDate(String symbol, LocalDate analysisDate);
    List<BTSTAnalysis> findByAnalysisDateAndRecommendationOrderByConfidenceScoreDesc(LocalDate analysisDate, String recommendation);
    List<BTSTAnalysis> findByAnalysisDate(LocalDate analysisDate);
}
