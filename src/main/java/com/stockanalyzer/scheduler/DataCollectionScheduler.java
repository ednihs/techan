package com.stockanalyzer.scheduler;

import com.stockanalyzer.repository.PriceDataRepository;
import com.stockanalyzer.service.BhavcopyService;
import com.stockanalyzer.service.TechnicalAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
    private final PriceDataRepository priceDataRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void preloadRecentHistory() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(99);
        boolean fetched = false;
        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
            if (isWeekend(date) || priceDataRepository.existsByTradeDate(date)) {
                continue;
            }
            log.info("Bootstrapping bhavcopy data for {}", date);
            bhavcopyService.downloadAndProcess(date);
            technicalAnalysisService.calculateIndicatorsForDate(date);
            fetched = true;
        }
        if (fetched) {
            log.info("Historical backfill completed. Calculating indicators for the latest session.");
            technicalAnalysisService.calculateDailyIndicators();
        }
    }

    @Scheduled(cron = "0 30 16 * * MON-FRI", zone = "Asia/Kolkata")
    public void collectEndOfDayData() {
        LocalDate tradeDate = LocalDate.now();
        if (isWeekend(tradeDate)) {
            return;
        }
        log.info("Starting data collection for {}", tradeDate);
        bhavcopyService.downloadAndProcess(tradeDate);
        technicalAnalysisService.calculateDailyIndicators();
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}
