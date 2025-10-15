package com.stockanalyzer.service;

import com.stockanalyzer.dto.HistoricalDataPoint;
import com.stockanalyzer.dto.MarketFeedData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FivePaisaService {

    private final WebClient.Builder webClientBuilder;

    @Value("${fivepaisa.api.base-url:https://openapi.5paisa.com/VendorsAPI/Service1.svc}")
    private String baseUrl;

    @Value("${fivepaisa.api.client-code:demo}")
    private String clientCode;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        authenticate();
    }

    public void authenticate() {
        log.info("Initializing FivePaisa client for code {}", clientCode);
    }

    public List<MarketFeedData> getMarketFeed(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Mono<List<Map<String, Object>>> response = webClient.post()
                    .uri("/marketfeed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Collections.singletonMap("symbols", symbols))
                    .retrieve()
                    .bodyToMono(List.class);
            List<Map<String, Object>> payload = response.blockOptional().orElse(Collections.emptyList());
            return payload.stream().map(this::mapMarketFeed).collect(Collectors.toList());
        } catch (Exception ex) {
            log.warn("Falling back to static market feed for {} symbols due to: {}", symbols.size(), ex.getMessage());
            return symbols.stream()
                    .map(symbol -> MarketFeedData.builder()
                            .symbol(symbol)
                            .lastTradedPrice(BigDecimal.ZERO)
                            .changePercent(BigDecimal.ZERO)
                            .volume(0)
                            .build())
                    .collect(Collectors.toList());
        }
    }

    public List<HistoricalDataPoint> getHistoricalData(String symbol, LocalDate fromDate, LocalDate toDate) {
        if (symbol == null) {
            return Collections.emptyList();
        }
        try {
            Mono<List<Map<String, Object>>> response = webClient.post()
                    .uri("/historicaldata")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "symbol", symbol,
                            "from", fromDate.toString(),
                            "to", toDate.toString()))
                    .retrieve()
                    .bodyToMono(List.class);
            List<Map<String, Object>> payload = response.blockOptional().orElse(Collections.emptyList());
            return payload.stream().map(this::mapHistoricalPoint).collect(Collectors.toList());
        } catch (Exception ex) {
            log.warn("Falling back to synthetic history for {} due to: {}", symbol, ex.getMessage());
            return Collections.emptyList();
        }
    }

    private MarketFeedData mapMarketFeed(Map<String, Object> payload) {
        return MarketFeedData.builder()
                .symbol(String.valueOf(payload.getOrDefault("symbol", "UNKNOWN")))
                .lastTradedPrice(new BigDecimal(String.valueOf(payload.getOrDefault("ltp", "0"))))
                .changePercent(new BigDecimal(String.valueOf(payload.getOrDefault("changePct", "0"))))
                .volume(Long.parseLong(String.valueOf(payload.getOrDefault("volume", "0"))))
                .build();
    }

    private HistoricalDataPoint mapHistoricalPoint(Map<String, Object> payload) {
        return HistoricalDataPoint.builder()
                .date(LocalDate.parse(String.valueOf(payload.getOrDefault("date", LocalDate.now().toString()))))
                .open(new BigDecimal(String.valueOf(payload.getOrDefault("open", "0"))))
                .high(new BigDecimal(String.valueOf(payload.getOrDefault("high", "0"))))
                .low(new BigDecimal(String.valueOf(payload.getOrDefault("low", "0"))))
                .close(new BigDecimal(String.valueOf(payload.getOrDefault("close", "0"))))
                .volume(Long.parseLong(String.valueOf(payload.getOrDefault("volume", "0"))))
                .build();
    }
}
