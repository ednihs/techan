package com.stockanalyzer.scheduler;

import com.stockanalyzer.repository.PriceDataRepository;
import com.stockanalyzer.service.BhavcopyService;
import com.stockanalyzer.service.TechnicalAnalysisService;
import jakarta.annotation.PostConstruct;
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

    private static final int LOOKBACK_DAYS = 100;

    private final BhavcopyService bhavcopyService;
    private final TechnicalAnalysisService technicalAnalysisService;
    private final PriceDataRepository priceDataRepository;

    @PostConstruct
    public void ensureRecentHistoryAvailable() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.minusDays(0);
        LocalDate startDate = endDate.minusDays(LOOKBACK_DAYS - 1);
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }
            if (priceDataRepository.existsByTradeDate(date)) {
                continue;
            }
            log.info("Missing price data for {}. Attempting backfill.", date);
            bhavcopyService.downloadAndProcess(date);
        }
    }

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
