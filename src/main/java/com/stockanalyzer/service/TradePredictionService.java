package com.stockanalyzer.service;

import com.stockanalyzer.entity.PriceData;
import com.stockanalyzer.ml.LstmTradesModel;
import com.stockanalyzer.ml.LstmTradesTrainer;
import com.stockanalyzer.repository.PriceDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
public class TradePredictionService {

    private static final Logger log = LoggerFactory.getLogger(TradePredictionService.class);
    private static final String MODEL_STORAGE_PATH = "data/models/";

    private final DataSource dataSource;
    private final PriceDataRepository priceDataRepository;

    @Autowired
    public TradePredictionService(DataSource dataSource, PriceDataRepository priceDataRepository) {
        this.dataSource = dataSource;
        this.priceDataRepository = priceDataRepository;
        new File(MODEL_STORAGE_PATH).mkdirs();
    }

    public void trainModelsForAllSymbols(int windowSize, double lambda) {
        log.info("Starting model training job for all symbols.");
        List<String> allSymbols = priceDataRepository.findDistinctSymbols();
        log.info("Found {} symbols to train.", allSymbols.size());
        for (String symbol : allSymbols) {
            trainAndSaveModel(symbol, windowSize, lambda);
        }
        log.info("Finished model training job for all {} symbols.", allSymbols.size());
    }

    public void trainAndSaveModel(String symbol, int windowSize, double learningRate) {
        log.info("Starting training for symbol: {}", symbol);
        try (java.sql.Connection conn = dataSource.getConnection()) {
            LstmTradesModel model = LstmTradesTrainer.trainForSymbol(conn, symbol, windowSize, learningRate);
            File modelFile = new File(getModelFilePath(symbol, windowSize));
            model.save(modelFile);
            log.info("Model for {} saved to {}", symbol, modelFile.getPath());
        } catch (java.sql.SQLException | java.io.IOException e) {
            log.error("Failed to train and save model for symbol {}: {}", symbol, e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("Could not train model for {}: {}", symbol, e.getMessage());
        }
    }

    public void predictAndUpdateTradesForAllSymbols(LocalDate date, int windowSize) {
        List<String> symbolsForDate = priceDataRepository.findDistinctSymbolsByTradeDate(date);
        log.info("Starting prediction for all {} symbols for date {}", symbolsForDate.size(), date);
        for (String symbol : symbolsForDate) {
            try {
                predictAndUpdateTrades(symbol, date, windowSize);
            } catch (Exception e) {
                log.error("Failed to predict trades for symbol {}: {}", symbol, e.getMessage());
            }
        }
        log.info("Finished prediction job for all symbols.");
    }

    public void predictAndUpdateTrades(String symbol, LocalDate date, int windowSize) throws Exception {
        File modelFile = new File(getModelFilePath(symbol, windowSize));
        if (!modelFile.exists()) {
            throw new IllegalStateException("Model file not found for " + symbol + ". Please train the model first.");
        }
        
        LstmTradesModel model;
        try {
            model = LstmTradesModel.load(modelFile, windowSize);
        } catch (IOException e) {
            // If the model is missing scaler files (old version), throw a clear error
            if (e.getMessage().contains("Scaler files not found")) {
                throw new IllegalStateException("Model for " + symbol + " is outdated and missing scaler files. " +
                        "Please retrain the model using the training endpoint.", e);
            }
            throw e;
        }

        List<PriceData> priceDataForDate = priceDataRepository.findBySymbolAndTradeDateBetween(symbol, date, date);
        if (priceDataForDate.isEmpty()) {
            throw new IllegalStateException("No price data for " + symbol + " on " + date);
        }
        PriceData targetPriceData = priceDataForDate.get(0);

        List<PriceData> history = priceDataRepository.findBySymbolAndTradeDateBeforeOrderByTradeDateDesc(symbol, date, PageRequest.of(0, windowSize));
        if (history.size() < windowSize) {
            throw new IllegalStateException("Not enough historical data for " + symbol + ". Need " + windowSize + ", got " + history.size());
        }
        Collections.reverse(history);

        double[] tradeHistory = history.stream().mapToDouble(PriceData::getNoOfTrades).toArray();
        double[] volumeHistory = new double[windowSize + 1];
        for (int i = 0; i < windowSize; i++) {
            volumeHistory[i] = history.get(i).getVolume();
        }
        volumeHistory[windowSize] = targetPriceData.getVolume();

        double predictedTrades = model.predict(tradeHistory, volumeHistory);

        log.info("Updating no_of_trades for {} on {} to predicted value {}", symbol, date, (int) Math.round(predictedTrades));
        targetPriceData.setNoOfTrades((int) Math.round(predictedTrades));
        priceDataRepository.save(targetPriceData);
    }

    private String getModelFilePath(String symbol, int windowSize) {
        return MODEL_STORAGE_PATH + symbol + "_lstm_trades_model_ws" + windowSize + ".zip";
    }
    
    /**
     * Deletes all old models that don't have scaler files (models trained with older version)
     * @param windowSize The window size used for models
     * @return Number of models deleted
     */
    public int deleteOldModelsWithoutScalers(int windowSize) {
        int deletedCount = 0;
        File modelsDir = new File(MODEL_STORAGE_PATH);
        if (!modelsDir.exists()) {
            return 0;
        }
        
        File[] modelFiles = modelsDir.listFiles((dir, name) -> 
            name.endsWith("_lstm_trades_model_ws" + windowSize + ".zip"));
        
        if (modelFiles == null) {
            return 0;
        }
        
        for (File modelFile : modelFiles) {
            String basePath = modelFile.getAbsolutePath();
            File tradeScalerFile = new File(basePath + ".trade_scaler");
            File volumeScalerFile = new File(basePath + ".volume_scaler");
            
            // Delete model if it's missing scaler files
            if (!tradeScalerFile.exists() || !volumeScalerFile.exists()) {
                log.info("Deleting old model without scalers: {}", modelFile.getName());
                if (modelFile.delete()) {
                    deletedCount++;
                    // Also delete any partial scaler files if they exist
                    if (tradeScalerFile.exists()) tradeScalerFile.delete();
                    if (volumeScalerFile.exists()) volumeScalerFile.delete();
                }
            }
        }
        
        log.info("Deleted {} old models without scaler files", deletedCount);
        return deletedCount;
    }
}
