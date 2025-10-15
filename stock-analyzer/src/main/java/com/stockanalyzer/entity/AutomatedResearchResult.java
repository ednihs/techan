package com.stockanalyzer.entity;

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
@Table(name = "automated_research_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutomatedResearchResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private LocalDate researchDate;
    private String catalystType;
    private String catalystDescription;
    private Integer catalystScore;
    private Double newsSentimentScore;
    private String researchSource;

    @CreatedDate
    private LocalDateTime createdAt;
}
