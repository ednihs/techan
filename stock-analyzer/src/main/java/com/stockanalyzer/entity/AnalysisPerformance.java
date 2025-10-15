package com.stockanalyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_performance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisPerformance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long analysisId;
    private String symbol;
    private LocalDate recommendationDate;
    private Double entryPrice;
    private Double exitPrice;
    private LocalDate exitDate;
    private String exitReason;
    private Double actualReturn;
    private Double predictedReturn;
    private Boolean success;

    @CreatedDate
    private LocalDateTime createdAt;
}
