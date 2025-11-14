package com.stockanalyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.stockanalyzer.dto.EnrichedTechnicalIndicatorDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class IndicatorExportService {

    private final TechnicalAnalysisService technicalAnalysisService;
    private final ObjectMapper objectMapper;

    @Value("${indicator.export.path}")
    private String exportPath;

    @Value("${indicator.export.rclone-sync-command}")
    private String rcloneSyncCommand;

    @Value("${indicator.export.csv.path}")
    private String csvExportPath;

    @Value("${indicator.export.csv.rclone-sync-command}")
    private String csvRcloneSyncCommand;

    @Autowired
    public IndicatorExportService(TechnicalAnalysisService technicalAnalysisService) {
        this.technicalAnalysisService = technicalAnalysisService;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void exportIndicatorsForDate(LocalDate date) {
        log.info("Starting indicator export for date: {}", date);

        File exportDir = new File(exportPath);
        if (!exportDir.exists()) {
            log.info("Creating export directory: {}", exportPath);
            exportDir.mkdirs();
        }

        List<EnrichedTechnicalIndicatorDTO> indicators = technicalAnalysisService.getEnrichedTechnicalIndicatorsForDate(date);

        if (indicators.isEmpty()) {
            log.warn("No indicators found for date: {}. Nothing to export.", date);
            return;
        }

        log.info("Found {} indicators to export.", indicators.size());

        for (EnrichedTechnicalIndicatorDTO indicator : indicators) {
            String symbol = indicator.getSymbol();
            String fileName = symbol + ".json";
            File outputFile = Paths.get(exportPath, fileName).toFile();

            try {
                objectMapper.writeValue(outputFile, indicator);
                log.debug("Successfully wrote indicator for {} to {}", symbol, outputFile.getAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to write indicator file for symbol: " + symbol, e);
            }
        }

        log.info("Finished exporting {} indicators to {}", indicators.size(), exportPath);

        // Execute rclone sync command
        executeShellCommand(rcloneSyncCommand);
    }

    public void exportIndicatorsToCsvForDate(LocalDate date) {
        log.info("Starting indicator CSV export for date: {}", date);

        File exportDir = new File(csvExportPath);
        if (!exportDir.exists()) {
            log.info("Creating CSV export directory: {}", csvExportPath);
            exportDir.mkdirs();
        }

        List<EnrichedTechnicalIndicatorDTO> indicators = technicalAnalysisService.getEnrichedTechnicalIndicatorsForDate(date);

        if (indicators.isEmpty()) {
            log.warn("No indicators found for date: {}. Nothing to export.", date);
            return;
        }

        String fileName = "indicators-" + date + ".csv";
        File outputFile = Paths.get(csvExportPath, fileName).toFile();

        try (Writer writer = new FileWriter(outputFile)) {
            StatefulBeanToCsv<EnrichedTechnicalIndicatorDTO> beanToCsv = new StatefulBeanToCsvBuilder<EnrichedTechnicalIndicatorDTO>(writer)
                    .withQuotechar('\"')
                    .withSeparator(',')
                    .withOrderedResults(false)
                    .build();
            beanToCsv.write(indicators);
            log.info("Successfully wrote {} indicators to CSV file: {}", indicators.size(), outputFile.getAbsolutePath());
        } catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            log.error("Failed to write indicators to CSV file", e);
            return; // Don't run sync if CSV writing fails
        }

        // Execute rclone sync for CSV
        executeShellCommand(csvRcloneSyncCommand);
    }

    private void executeShellCommand(String command) {
        if (command == null || command.isBlank()) {
            log.warn("Shell command is empty, skipping execution.");
            return;
        }

        log.info("Executing shell command: {}", command);
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Shell command executed successfully.");
            } else {
                log.error("Shell command failed with exit code: {}", exitCode);
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.error(line);
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            log.error("Exception occurred while executing shell command: " + command, e);
            Thread.currentThread().interrupt();
        }
    }
}
