package com.stockanalyzer.scheduler;

import com.stockanalyzer.service.TradePredictionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ModelTrainingScheduler {

    private static final Logger log = LoggerFactory.getLogger(ModelTrainingScheduler.class);

    @Autowired
    private TradePredictionService tradePredictionService;

    // This scheduler is currently disabled. Uncomment the @Scheduled annotation to enable it.
    // It will run every Sunday at 2 AM to retrain models for all stocks.
    // @Scheduled(cron = "0 0 2 * * SUN", zone = "Asia/Kolkata")
    public void retrainAllModels() {
        log.info("Starting weekly model retraining job.");
        int windowSize = 20;
        double lambda = 0.01;
        tradePredictionService.trainModelsForAllSymbols(windowSize, lambda);
        log.info("Finished weekly model retraining job.");
    }
}
