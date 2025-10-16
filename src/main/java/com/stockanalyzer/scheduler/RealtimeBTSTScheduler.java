package com.stockanalyzer.scheduler;

import com.stockanalyzer.service.BTSTAnalysisService;
import com.stockanalyzer.service.BhavcopyService;
import com.stockanalyzer.service.IntradayDataService;
import com.stockanalyzer.service.RealtimeWeakHandsService;
import com.stockanalyzer.service.TechnicalAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RealtimeBTSTScheduler {

    private final IntradayDataService intradayDataService;
    private final RealtimeWeakHandsService realtimeWeakHandsService;
    private final BTSTAnalysisService btstAnalysisService;
    private final BhavcopyService bhavcopyService;
    private final TechnicalAnalysisService technicalAnalysisService;

    @Scheduled(cron = "0 30 16 * * MON-FRI", zone = "Asia/Kolkata")
    public void downloadBhavcopy() {
        log.info("Downloading daily bhavcopy for realtime pipeline");
        bhavcopyService.downloadAndProcess(LocalDate.now());
    }

    @Scheduled(cron = "0 0 17 * * MON-FRI", zone = "Asia/Kolkata")
    public void calculateDailyTechnicals() {
        log.info("Calculating daily indicators for realtime workflow");
        technicalAnalysisService.calculateDailyIndicators();
    }

    @Scheduled(cron = "0 30 17 * * MON-FRI", zone = "Asia/Kolkata")
    public void identifyDayOneCandidates() {
        LocalDate today = LocalDate.now();
        List<String> candidates = btstAnalysisService.identifyAndStoreBTSTCandidates(today);
        log.info("Identified {} BTST day-1 candidates for {}", candidates.size(), today.plusDays(1));
    }

    @Scheduled(cron = "0 */3 9-15 * * MON-FRI", zone = "Asia/Kolkata")
    public void collectRealtimeData() {
        if (!isMarketOpen()) {
            return;
        }
        List<String> watchlist = btstAnalysisService.getYesterdayBTSTCandidates();
        if (watchlist.isEmpty()) {
            log.debug("No watchlist symbols available for realtime collection");
            return;
        }
        intradayDataService.fetchAndStoreIntradayData(watchlist);
    }

    @Scheduled(cron = "0 0 10 * * MON-FRI", zone = "Asia/Kolkata")
    public void runMorningAnalysis() {
        LocalDate today = LocalDate.now();
        for (String symbol : btstAnalysisService.getYesterdayBTSTCandidates()) {
            realtimeWeakHandsService.analyzeMorningSession(symbol, today);
        }
    }

    @Scheduled(cron = "0 0 12 * * MON-FRI", zone = "Asia/Kolkata")
    public void runMidSessionAnalysis() {
        LocalDate today = LocalDate.now();
        for (String symbol : btstAnalysisService.getYesterdayBTSTCandidates()) {
            realtimeWeakHandsService.analyzeMidSession(symbol, today);
        }
    }

    @Scheduled(cron = "0 0 14 * * MON-FRI", zone = "Asia/Kolkata")
    public void generateAfternoonSignals() {
        LocalDate today = LocalDate.now();
        for (String symbol : btstAnalysisService.getYesterdayBTSTCandidates()) {
            realtimeWeakHandsService.analyzeAfternoonSession(symbol, today);
        }
        btstAnalysisService.generateRealtimeRecommendations(today);
    }

    @Scheduled(cron = "0 45 14 * * MON-FRI", zone = "Asia/Kolkata")
    public void refreshSignals() {
        btstAnalysisService.refreshTopRecommendations(LocalDate.now());
    }

    @Scheduled(cron = "0 0 18 * * MON-FRI", zone = "Asia/Kolkata")
    public void validateSignals() {
        btstAnalysisService.validateRealtimeSignals(LocalDate.now());
    }

    private boolean isMarketOpen() {
        LocalTime now = LocalTime.now();
        return !now.isBefore(LocalTime.of(9, 15)) && now.isBefore(LocalTime.of(15, 30));
    }
}
