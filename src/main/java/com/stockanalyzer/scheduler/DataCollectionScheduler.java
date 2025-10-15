package com.stockanalyzer.scheduler;

import com.stockanalyzer.service.BhavcopyService;
import com.stockanalyzer.service.TechnicalAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class DataCollectionScheduler {

    private final BhavcopyService bhavcopyService;
    private final TechnicalAnalysisService technicalAnalysisService;

    @Scheduled(cron = "0 10 12 * * MON-FRI", zone = "Asia/Kolkata")
    public void collectEndOfDayData() {
        LocalDate tradeDate = LocalDate.now();
        if (tradeDate.getDayOfWeek() == DayOfWeek.SATURDAY || tradeDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return;
        }
        log.info("Starting data collection for {}", tradeDate);
        bhavcopyService.downloadAndProcess(tradeDate);
        technicalAnalysisService.calculateDailyIndicators();
    }
}
