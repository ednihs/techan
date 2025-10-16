package com.stockanalyzer.service;

import com.opencsv.CSVReader;
import com.stockanalyzer.entity.PriceData;
import com.stockanalyzer.repository.PriceDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BhavcopyService {

    private final WebClient.Builder webClientBuilder;
    private final PriceDataRepository priceDataRepository;

    @Value("${nse.bhavcopy.base-url:https://nsearchives.nseindia.com/content/cm/}")
    private String baseUrl;

    @Value("${nse.bhavcopy.file-pattern:BhavCopy_NSE_CM_0_0_0_{date}_F_0000.csv.zip}")
    private String filePattern;

    public void downloadAndProcess(LocalDate date) {
        String formattedDate = date.toString().replaceAll("-", "");
        String fileName = filePattern.replace("{date}", formattedDate);
        String url = baseUrl + fileName;
        log.info("Downloading bhavcopy from {}", url);
        try {
            byte[] zipBytes = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .blockOptional()
                    .orElseThrow(() -> new IllegalStateException("No response received"));
            String csvContent = extractZipContent(zipBytes);
            List<PriceData> priceDataList = parseBhavcopyCSV(csvContent, date);
            priceDataRepository.saveAll(priceDataList);
            log.info("Persisted {} bhavcopy records for {}", priceDataList.size(), date);
        } catch (Exception ex) {
            log.error("Failed to process bhavcopy for {}: {}", date, ex.getMessage());
        }
    }

    private String extractZipContent(byte[] zipData) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipData))) {
            if (zis.getNextEntry() == null) {
                return "";
            }
            return new String(zis.readAllBytes());
        }
    }

    List<PriceData> parseBhavcopyCSV(String csvContent, LocalDate tradeDate) {
        List<PriceData> priceDataList = new ArrayList<>();
        if (csvContent == null || csvContent.isEmpty()) {
            return priceDataList;
        }
        try (CSVReader reader = new CSVReader(new StringReader(csvContent))) {
            reader.readNext(); // skip header
            String[] line;
            while ((line = reader.readNext()) != null) {
                PriceData priceData = parseLine(line, tradeDate);
                if (priceData != null) {
                    priceDataList.add(priceData);
                }
            }
        } catch (Exception ex) {
            log.error("Error parsing bhavcopy CSV: {}", ex.getMessage());
        }
        return priceDataList;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.trim());
    }

    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        return Long.parseLong(value.trim());
    }

    private Integer parseInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        return Integer.parseInt(value.trim());
    }

    private PriceData parseLine(String[] fields, LocalDate tradeDate) {
        if (fields == null || fields.length < 27) {
            return null;
        }
        String symbol = fields[7].trim();
        String series = fields[8].trim();
        if (!"EQ".equalsIgnoreCase(series)) {
            return null;
        }
        try {
            return PriceData.builder()
                    .symbol(symbol)
                    .tradeDate(tradeDate)
                    .openPrice(parseBigDecimal(fields[14]))
                    .highPrice(parseBigDecimal(fields[15]))
                    .lowPrice(parseBigDecimal(fields[16]))
                    .closePrice(parseBigDecimal(fields[17]))
                    .prevClosePrice(parseBigDecimal(fields[19]))
                    .volume(parseLong(fields[24]))
                    .valueTraded(parseBigDecimal(fields[25]).longValue())
                    .noOfTrades(parseInt(fields[26]))
                    .build();
        } catch (NumberFormatException e) {
            log.warn("Skipping line for symbol {} due to number format error: {}", symbol, e.getMessage());
            return null;
        }
    }
}