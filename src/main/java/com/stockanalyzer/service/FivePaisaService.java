package com.stockanalyzer.service;

import com.stockanalyzer.dto.HistoricalDataPoint;
import com.stockanalyzer.dto.MarketFeedData;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
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

    @Value("${fivepaisa.api.app-name:demo}")
    private String appName;

    @Value("${fivepaisa.api.encrypt-key:demo}")
    private String encryptKey;

    @Value("${fivepaisa.api.login-id:demo}")
    private String loginId;

    @Value("${fivepaisa.api.request-token:}")
    private String initialRequestToken;

    private WebClient webClient;

    private final AtomicReference<String> bearerToken = new AtomicReference<>("");
    private final AtomicReference<String> refreshToken = new AtomicReference<>("");
    private final AtomicReference<String> feedToken = new AtomicReference<>("");
    private final AtomicReference<String> requestToken = new AtomicReference<>("");
    private final AtomicReference<Instant> tokenExpiry = new AtomicReference<>(Instant.EPOCH);

    private final Object authMonitor = new Object();

    private static final Duration AUTH_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration TOKEN_EXPIRY_GRACE = Duration.ofMinutes(1);

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        if (StringUtils.hasText(initialRequestToken)) {
            requestToken.set(initialRequestToken.trim());
        }
        authenticate();
    }

    public void authenticate() {
        if (hasValidToken()) {
            return;
        }

        synchronized (authMonitor) {
            if (hasValidToken()) {
                return;
            }

            if (!StringUtils.hasText(appName) || !StringUtils.hasText(encryptKey) || !StringUtils.hasText(loginId)) {
                log.warn("Skipping FivePaisa authentication because mandatory credentials are missing (appName/loginId/encryptKey)");
                return;
            }

            String currentRequestToken = requestToken.get();
            if (!StringUtils.hasText(currentRequestToken)) {
                log.warn("FivePaisa RequestToken not configured. Complete the OAuth login flow and call updateRequestToken() before attempting API calls.");
                return;
            }

            Map<String, Object> head = new HashMap<>();
            head.put("Key", appName);

            Map<String, Object> body = new HashMap<>();
            body.put("RequestToken", currentRequestToken);
            body.put("EncryKey", encryptKey);
            body.put("UserId", loginId);
            if (StringUtils.hasText(clientCode)) {
                body.put("ClientCode", clientCode);
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("head", head);
            payload.put("body", body);

            try {
                Mono<Map<String, Object>> responseMono = webClient.post()
                        .uri("/GetAccessToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                        });

                Map<String, Object> response = responseMono.blockOptional(AUTH_TIMEOUT).orElse(Collections.emptyMap());
                Map<String, Object> bodyMap = asMap(response.get("body"));

                String accessToken = extractString(bodyMap.get("AccessToken"));
                if (!StringUtils.hasText(accessToken)) {
                    log.warn("FivePaisa authentication response did not include an access token. Head: {}", asMap(response.get("head")));
                    return;
                }

                bearerToken.set(accessToken);
                refreshToken.set(extractString(bodyMap.get("RefreshToken")));
                feedToken.set(extractString(bodyMap.get("FeedToken")));

                Instant expiry = resolveExpiryInstant(bodyMap);
                tokenExpiry.set(expiry);

                this.webClient = this.webClient.mutate()
                        .defaultHeaders(headers -> {
                            headers.setBearerAuth(accessToken);
                            headers.add("clientCode", clientCode);
                            if (StringUtils.hasText(feedToken.get())) {
                                headers.add("feedToken", feedToken.get());
                            }
                        })
                        .build();

                log.info("FivePaisa authentication succeeded for client {}. Token valid until {}", clientCode, tokenExpiry.get());
            } catch (Exception ex) {
                log.warn("Failed to authenticate with FivePaisa for client {}: {}", clientCode, ex.getMessage());
            }
        }
    }

    public void updateRequestToken(String newToken) {
        if (!StringUtils.hasText(newToken)) {
            return;
        }
        synchronized (authMonitor) {
            requestToken.set(newToken.trim());
            bearerToken.set("");
            refreshToken.set("");
            feedToken.set("");
            tokenExpiry.set(Instant.EPOCH);
        }
    }

    public List<MarketFeedData> getMarketFeed(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyList();
        }
        ensureAuthenticated();
        try {
            Mono<List<Map<String, Object>>> response = webClient.post()
                    .uri("/marketfeed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Collections.singletonMap("symbols", symbols))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
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
        ensureAuthenticated();
        try {
            Mono<List<Map<String, Object>>> response = webClient.post()
                    .uri("/historicaldata")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "symbol", symbol,
                            "from", fromDate.toString(),
                            "to", toDate.toString()))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
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

    private void ensureAuthenticated() {
        if (!hasValidToken()) {
            authenticate();
        }
    }

    private boolean hasValidToken() {
        Instant expiry = tokenExpiry.get();
        return StringUtils.hasText(bearerToken.get()) && expiry != null && Instant.now().isBefore(expiry.minus(TOKEN_EXPIRY_GRACE));
    }

    private long parseLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String extractString(Object value) {
        if (value == null) {
            return "";
        }
        String result = String.valueOf(value);
        return "null".equalsIgnoreCase(result) ? "" : result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> target = new HashMap<>();
            map.forEach((k, v) -> target.put(String.valueOf(k), v));
            return target;
        }
        return Collections.emptyMap();
    }

    private Instant resolveExpiryInstant(Map<String, Object> bodyMap) {
        long expiresIn = parseLong(bodyMap.get("AccessTokenExpiry"), -1L);
        if (expiresIn <= 0) {
            expiresIn = parseLong(bodyMap.get("ExpiresIn"), 600L);
        }
        return Instant.now().plusSeconds(Math.max(expiresIn, 60L));
    }
}
