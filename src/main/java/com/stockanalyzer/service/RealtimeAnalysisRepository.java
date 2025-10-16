package com.stockanalyzer.service;

import com.stockanalyzer.service.RealtimeWeakHandsService.AfternoonAnalysis;
import com.stockanalyzer.service.RealtimeWeakHandsService.MidSessionAnalysis;
import com.stockanalyzer.service.RealtimeWeakHandsService.MorningAnalysis;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RealtimeAnalysisRepository {

    private final Map<LocalDate, Map<String, MorningAnalysis>> morningAnalyses = new ConcurrentHashMap<>();
    private final Map<LocalDate, Map<String, MidSessionAnalysis>> midSessionAnalyses = new ConcurrentHashMap<>();
    private final Map<LocalDate, Map<String, AfternoonAnalysis>> afternoonAnalyses = new ConcurrentHashMap<>();

    public void saveMorningAnalysis(MorningAnalysis analysis) {
        morningAnalyses.computeIfAbsent(analysis.getAnalysisDate(), key -> new ConcurrentHashMap<>())
                .put(analysis.getSymbol(), analysis);
    }

    public MorningAnalysis findMorningAnalysis(String symbol, LocalDate date) {
        return morningAnalyses.getOrDefault(date, Collections.emptyMap()).get(symbol);
    }

    public void saveMidSessionAnalysis(MidSessionAnalysis analysis) {
        midSessionAnalyses.computeIfAbsent(analysis.getAnalysisDate(), key -> new ConcurrentHashMap<>())
                .put(analysis.getSymbol(), analysis);
    }

    public MidSessionAnalysis findMidSessionAnalysis(String symbol, LocalDate date) {
        return midSessionAnalyses.getOrDefault(date, Collections.emptyMap()).get(symbol);
    }

    public void saveAfternoonAnalysis(AfternoonAnalysis analysis) {
        afternoonAnalyses.computeIfAbsent(analysis.getAnalysisDate(), key -> new ConcurrentHashMap<>())
                .put(analysis.getSymbol(), analysis);
    }

    public AfternoonAnalysis findAfternoonAnalysis(String symbol, LocalDate date) {
        return afternoonAnalyses.getOrDefault(date, Collections.emptyMap()).get(symbol);
    }

    public List<AfternoonAnalysis> findAfternoonAnalysesWithBuySignal(LocalDate date) {
        return new ArrayList<>(afternoonAnalyses.getOrDefault(date, Collections.emptyMap())
                .values()
                .stream()
                .filter(AfternoonAnalysis::isBuySignal)
                .collect(java.util.stream.Collectors.toList()));
    }

    public void clear(LocalDate date) {
        morningAnalyses.remove(date);
        midSessionAnalyses.remove(date);
        afternoonAnalyses.remove(date);
    }
}
