package com.stockanalyzer.scheduler;

import com.stockanalyzer.service.BTSTAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalysisScheduler {

    private final BTSTAnalysisService btstAnalysisService;

    @Scheduled(cron = "0 0 18 * * MON-FRI", zone = "Asia/Kolkata")
    public void executeAnalysis() {
        log.info("Triggering scheduled BTST analysis run");
        btstAnalysisService.runDailyAnalysis();
    }
}
