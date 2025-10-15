package com.stockanalyzer.scheduler;

import com.stockanalyzer.service.BhavcopyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Slf4j
public class DataCollectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(DataCollectionScheduler.class);
    private final BhavcopyService bhavcopyService;

    public DataCollectionScheduler(BhavcopyService bhavcopyService) {
        this.bhavcopyService = bhavcopyService;
    }

    @Scheduled(cron = "0 30 16 * * MON-FRI") // 4:30 PM on weekdays
    public void downloadDailyBhavcopy() {
        log.info("Starting daily bhavcopy download...");
        // bhavcopyService.downloadDailyBhavcopy();
    }
}
