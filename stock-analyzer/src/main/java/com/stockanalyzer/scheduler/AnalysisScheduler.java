package com.stockanalyzer.scheduler;

import com.stockanalyzer.service.BTSTAnalysisService;
import com.stockanalyzer.service.TechnicalAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Slf4j
public class AnalysisScheduler {

    private static final Logger log = LoggerFactory.getLogger(AnalysisScheduler.class);
    private final TechnicalAnalysisService technicalAnalysisService;
    private final BTSTAnalysisService btstAnalysisService;

    public AnalysisScheduler(TechnicalAnalysisService technicalAnalysisService, BTSTAnalysisService btstAnalysisService) {
        this.technicalAnalysisService = technicalAnalysisService;
        this.btstAnalysisService = btstAnalysisService;
    }

    @Scheduled(cron = "0 0 17 * * MON-FRI") // 5:00 PM on weekdays
    public void runDailyAnalysis() {
        log.info("Starting daily technical analysis...");
        // technicalAnalysisService.calculateDailyIndicators();
    }

    @Scheduled(cron = "0 0 18 * * MON-FRI") // 6:00 PM on weekdays
    public void runDailyBTSTAnalysis() {
        log.info("Starting daily BTST analysis...");
        // btstAnalysisService.runDailyBTSTAnalysis();
    }
}
