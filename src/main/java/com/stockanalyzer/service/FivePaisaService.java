package com.stockanalyzer.service;

import com.FivePaisa.api.RestClient;
import com.FivePaisa.config.AppConfig;
import com.FivePaisa.service.Properties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockanalyzer.dto.AccessTokenRequest;
import com.stockanalyzer.dto.AccessTokenResponse;
import com.stockanalyzer.dto.HistoricalDataPoint;
import com.stockanalyzer.dto.HoldingDTO;
import com.stockanalyzer.dto.MarketFeedData;
import com.stockanalyzer.dto.OrderRequestDTO;
import com.stockanalyzer.dto.OrderResponseDTO;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class FivePaisaService {

    private final ScripMasterService scripMasterService;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final WebClient.Builder webClientBuilder;

    @Value("${fivepaisa.api.base-url:https://openapi.5paisa.com/VendorsAPI/Service1.svc}")
    private String baseUrl;

    @Value("${fivepaisa.api.client-code:demo}")
    private String clientCode;

    // New Properties for OAuth
    @Value("${fivepaisa.app.key}")
    private String appKey;
    @Value("${fivepaisa.app.encrypt_key}")
    private String appEncryptKey;
    @Value("${fivepaisa.app.user_id}")
    private String appUserId;
    @Value("${fivepaisa.app.redirect_url}")
    private String redirectUrl;

    // --- End of new properties ---

    @Value("${fivepaisa.api.app-name:demo}")
    private String appName;

    @Value("${fivepaisa.api.app-version:1.0}")
    private String appVersion;

    @Value("${fivepaisa.api.os-name:WEB}")
    private String osName;

    @Value("${fivepaisa.api.encrypt-key:demo}")
    private String encryptKey;

    @Value("${fivepaisa.api.user-key:}")
    private String userKey;

    @Value("${fivepaisa.api.user-id:}")
    private String userId;

    @Value("${fivepaisa.api.password:}")
    private String password;

    @Value("${fivepaisa.api.login-id:demo}")
    private String loginId;

    @Value("${fivepaisa.api.pin:685561}")
    private String tradingPin;

    @Value("${fivepaisa.api.totp-secret:}")
    private String totpSecret;

    @Value("${fivepaisa.api.totp-code:}")
    private String staticTotpCode;

    @Value("${fivepaisa.api.totp-digits:6}")
    private int totpDigits;

    @Value("${fivepaisa.api.totp-step-seconds:30}")
    private int totpStepSeconds;

    @Value("${fivepaisa.api.device-ip:}")
    private String deviceIp;

    @Value("${fivepaisa.api.device-id:}")
    private String deviceId;

    @Value("${fivepaisa.api.app-source:11062}")
    private int appSource;

    private WebClient webClient;

    private final AtomicReference<String> bearerToken = new AtomicReference<>("");
    private final AtomicReference<Instant> tokenExpiry = new AtomicReference<>(Instant.EPOCH);

    private final Object authMonitor = new Object();

    private static final Duration TOKEN_EXPIRY_GRACE = Duration.ofMinutes(1);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketFeedRequestItem {
        private String exch;
        private String exchType;
        private String symbol;
        private String expiry = "";
        private String strikePrice = "0";
        private String optionType = "";

        public MarketFeedRequestItem(String exch, String exchType, String symbol) {
            this.exch = exch;
            this.exchType = exchType;
            this.symbol = symbol;
        }
    }

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        // The old automatic login is removed
        // authenticate(null);
    }

    public String getLoginUrl() {
        return String.format("https://dev-openapi.5paisa.com/WebVendorLogin/VLogin/Index?VendorKey=%s&ResponseURL=%s", appKey, redirectUrl);
    }

    public void generateAccessToken(String requestToken) {
        if (requestToken == null || requestToken.isBlank()) {
            throw new IllegalArgumentException("Request token cannot be empty.");
        }

        if (hasValidToken()) {
            log.info("Access token is still valid. Skipping generation.");
            return;
        }

        synchronized (authMonitor) {
            if (hasValidToken()) {
                return;
            }
            
            AccessTokenRequest.Head head = new AccessTokenRequest.Head(appKey);
            AccessTokenRequest.Body body = new AccessTokenRequest.Body(requestToken.trim(), appEncryptKey, appUserId);
            AccessTokenRequest accessTokenRequest = new AccessTokenRequest();
            accessTokenRequest.setHead(head);
            accessTokenRequest.setBody(body);

            // Step 4: Validate All Inputs Are Non-Null
            log.info("Attempting to generate access token with the following details:");
            log.info("--> App Key: {}", appKey);
            log.info("--> Request Token: {}", requestToken.trim());
            log.info("--> App Encrypt Key: {}", appEncryptKey);
            log.info("--> App User ID: {}", appUserId);


            int maxRetries = 3;
            for (int attempt = 0; attempt < maxRetries; attempt++) {
                try {
                    // Step 2: Log Your Exact JSON Request in Java
                    String jsonPayload = OBJECT_MAPPER.writeValueAsString(accessTokenRequest);
                    log.info("GetAccessToken Request Payload (Attempt {}/{}): {}", attempt + 1, maxRetries, jsonPayload);

                    AccessTokenResponse response = webClient.post()
                            .uri("/GetAccessToken")
                            .bodyValue(accessTokenRequest)
                            .retrieve()
                            .bodyToMono(AccessTokenResponse.class)
                            .block();

                    if (response != null && response.getBody() != null && response.getBody().getAccessToken() != null && response.getBody().getStatus() == 0) {
                        String token = response.getBody().getAccessToken();
                        bearerToken.set(token);
                        tokenExpiry.set(resolveExpiryInstant(null)); // Set expiry to end of day

                        this.webClient = this.webClient.mutate()
                                .defaultHeaders(headers -> {
                                    headers.setBearerAuth(token);
                                    headers.add("clientCode", clientCode);
                                })
                                .build();

                        log.info("Successfully generated and stored 5paisa access token. Valid until {}", tokenExpiry.get());
                        return; // Exit method on success

                    } else {
                        String message = response != null && response.getBody() != null ? response.getBody().getMessage() : "No response body";
                        log.error("Failed to generate access token from 5paisa. Response: {}", message);
                        throw new RuntimeException("Failed to get access token from 5paisa: " + message);
                    }
                } catch (WebClientResponseException ex) {
                    log.error("WebClient error during token generation: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
                    if (ex.getStatusCode().value() == 503 && attempt < maxRetries - 1) {
                        log.warn("503 Service Unavailable received. Retrying in {}s...", (long) Math.pow(2, attempt));
                        try {
                            Thread.sleep((long) Math.pow(2, attempt) * 1000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Retry wait was interrupted", ie);
                        }
                    } else {
                        throw new RuntimeException("Failed to get access token from 5paisa after retries.", ex);
                    }
                } catch (Exception e) {
                    log.error("Exception while generating access token", e);
                    throw new RuntimeException("Exception while generating access token", e);
                }
            }
            // If the loop completes without success
            throw new RuntimeException("Failed to generate access token after " + maxRetries + " attempts.");
        }
    }

    @SuppressWarnings("unchecked")
    public List<MarketFeedData> getMarketFeed(List<MarketFeedRequestItem> requestItems) {
        if (requestItems == null || requestItems.isEmpty()) {
            return Collections.emptyList();
        }
        ensureAuthenticated();

        AppConfig config = new AppConfig();
        config.setAppName(appName);
        config.setAppVer(appVersion);
        config.setOsName(osName);
        config.setEncryptKey(encryptKey);
        config.setKey(userKey);
        config.setUserId(userId);
        config.setPassword(password);
        config.setLoginId(loginId);

        Properties properties = new Properties();
        properties.setClientcode(clientCode);
        properties.setAppSource(appSource);
        if (StringUtils.hasText(deviceIp)) {
            properties.setRemoteIpAddress(deviceIp);
        }
        if (StringUtils.hasText(deviceId)) {
            properties.setMachineID(deviceId);
        }

        RestClient apis = new RestClient(config, properties);
        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        JSONObject requestJson = new JSONObject();
        requestJson.put("Count", requestItems.size());
        requestJson.put("ClientLoginType", 0);
        requestJson.put("LastRequestTime", "/Date(" +ZonedDateTime.now(istZone).with(LocalTime.MIN).toInstant().toEpochMilli() + ")/");
        requestJson.put("RefreshRate", "H");

        JSONArray marketFeedDataList = new JSONArray();
        for (MarketFeedRequestItem item : requestItems) {
            JSONObject itemJson = new JSONObject();
            itemJson.put("Exch", item.getExch());
            itemJson.put("ExchType", item.getExchType());
            itemJson.put("Symbol", item.getSymbol());
            itemJson.put("Expiry", item.getExpiry());
            itemJson.put("StrikePrice", item.getStrikePrice());
            itemJson.put("OptionType", item.getOptionType());
            marketFeedDataList.add(itemJson);
        }
        requestJson.put("MarketFeedData", marketFeedDataList);

        try {
            Response response = apis.marketFeed(requestJson);
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                Map<String, Object> responseMap = parseJson(responseBody);
                Map<String, Object> bodyMap = asMap(responseMap.get("body"));
                Object data = bodyMap.get("Data");
                if (data instanceof List) {
                    List<Map<String, Object>> marketData = new ArrayList<>();
                    for (Object item : (List<?>) data) {
                        marketData.add(asMap(item));
                    }
                    return marketData.stream()
                            .map(this::mapMarketFeedFromApiResponse)
                            .collect(Collectors.toList());
                }
            } else {
                log.warn("Failed to fetch market feed: {}", response.message());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch market feed", e);
        }

        return Collections.emptyList();
    }

    public List<com.stockanalyzer.entity.PriceData> getHistoricalData(int scripCode, String interval, LocalDate fromDate, LocalDate toDate) {
        ensureAuthenticated();
        String url = String.format("https://openapi.5paisa.com/V2/historical/M/D/%d/%s?from=%s&end=%s",
                scripCode, interval, fromDate.toString(), toDate.toString());

        try {
            String responseJson = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            Map<String, Object> responseMap = parseJson(responseJson);
            if (responseMap.containsKey("data")) {
                Map<String, Object> dataMap = asMap(responseMap.get("data"));
                if (dataMap.containsKey("candles")) {
                    @SuppressWarnings("unchecked")
                    List<List<Object>> candles = (List<List<Object>>) dataMap.get("candles");
                    return candles.stream()
                            .map(this::mapCandleToPriceData)
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch historical data for scripCode {}", scripCode, e);
        }
        return new ArrayList<>();
    }

    private com.stockanalyzer.entity.PriceData mapCandleToPriceData(List<Object> candle) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime timestamp = LocalDateTime.parse(String.valueOf(candle.get(0)), formatter);

        return com.stockanalyzer.entity.PriceData.builder()
                .tradeDate(timestamp.toLocalDate())
                .createdAt(timestamp)
                .openPrice(new BigDecimal(String.valueOf(candle.get(1))))
                .highPrice(new BigDecimal(String.valueOf(candle.get(2))))
                .lowPrice(new BigDecimal(String.valueOf(candle.get(3))))
                .closePrice(new BigDecimal(String.valueOf(candle.get(4))))
                .volume(((Number) candle.get(5)).longValue())
                .build();
    }

    public List<HoldingDTO> getHoldings() {
        ensureAuthenticated();
        log.info("Fetching holdings for client {}", clientCode);

        try {
            Map<String, Object> requestBody = Map.of(
                    "head", Map.of("key", userKey),
                    "body", Map.of("ClientCode", clientCode)
            );

            String responseJson = webClient.post()
                    .uri("/V3/Holding")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            Map<String, Object> responseMap = parseJson(responseJson);
            Map<String, Object> bodyMap = asMap(responseMap.get("body"));
            Object data = bodyMap.get("Data");

            if (data instanceof List) {
                List<Map<String, Object>> holdingsData = new ArrayList<>();
                for (Object item : (List<?>) data) {
                    holdingsData.add(asMap(item));
                }
                return holdingsData.stream()
                        .map(this::mapHoldingFromApiResponse)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch holdings", e);
        }
        return Collections.emptyList();
    }

    public OrderResponseDTO placeOrder(OrderRequestDTO orderRequest) {
        ensureAuthenticated();
        log.info("Placing order for client {}: {}", clientCode, orderRequest);

        try {
            String scripCode = scripMasterService.getScripCode(orderRequest.getSymbol());
            if (scripCode == null) {
                return OrderResponseDTO.builder().status("Failed").message("Invalid symbol").build();
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("head", Map.of("key", userKey));
            Map<String, Object> body = new HashMap<>();
            body.put("ClientCode", clientCode);
            body.put("Exchange", orderRequest.getExchange());
            body.put("ExchangeType", "C"); // Assuming Cash
            body.put("ScripCode", scripCode);
            body.put("Price", orderRequest.getPrice().doubleValue());
            body.put("OrderType", orderRequest.getTransactionType());
            body.put("Qty", orderRequest.getQuantity());
            body.put("DisQty", 0);
            body.put("IsIntraday", "DELIVERY".equalsIgnoreCase(orderRequest.getProductType()));
            body.put("iOrderValidity", 0); // Day order
            body.put("AHPlaced", "N");
            requestBody.put("body", body);

            String responseJson = webClient.post()
                    .uri("/V1/PlaceOrderRequest")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            Map<String, Object> responseMap = parseJson(responseJson);
            Map<String, Object> responseBody = asMap(responseMap.get("body"));

            return OrderResponseDTO.builder()
                    .orderId(String.valueOf(responseBody.get("BrokerOrderID")))
                    .status(String.valueOf(responseBody.get("Status")))
                    .message(String.valueOf(responseBody.get("Message")))
                    .build();

        } catch (Exception e) {
            log.warn("Failed to place order", e);
            return OrderResponseDTO.builder().status("Failed").message(e.getMessage()).build();
        }
    }

    public OrderResponseDTO cancelOrder(String orderId) {
        ensureAuthenticated();
        log.info("Cancelling order {} for client {}", orderId, clientCode);

        try {
            Map<String, Object> requestBody = Map.of(
                    "head", Map.of("key", userKey),
                    "body", Map.of("ClientCode", clientCode, "BrokerOrderID", orderId)
            );

            // The exact endpoint for cancel order is not confirmed from docs, assuming this based on pattern
            String responseJson = webClient.post()
                    .uri("/V1/CancelOrderRequest")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            Map<String, Object> responseMap = parseJson(responseJson);
            Map<String, Object> responseBody = asMap(responseMap.get("body"));

            return OrderResponseDTO.builder()
                    .orderId(orderId)
                    .status(String.valueOf(responseBody.get("Status")))
                    .message(String.valueOf(responseBody.get("Message")))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to cancel order", e);
            return OrderResponseDTO.builder().orderId(orderId).status("Failed").message(e.getMessage()).build();
        }
    }

    private String getScripCodeForSymbol(String symbol) {
        Map<String, String> symbolToScrip = new HashMap<>();
        symbolToScrip.put("TCS", "2885");
        symbolToScrip.put("RELIANCE", "2475");
        symbolToScrip.put("INFY", "1594");
        return symbolToScrip.get(symbol.toUpperCase());
    }

    private HoldingDTO mapHoldingFromApiResponse(Map<String, Object> payload) {
        BigDecimal avgPrice = new BigDecimal(String.valueOf(payload.getOrDefault("AvgRate", "0")));
        BigDecimal lastPrice = new BigDecimal(String.valueOf(payload.getOrDefault("CurrentPrice", "0")));
        int quantity = Integer.parseInt(String.valueOf(payload.getOrDefault("Quantity", "0")));
        BigDecimal pnl = lastPrice.subtract(avgPrice).multiply(new BigDecimal(quantity));

        return HoldingDTO.builder()
                .symbol(String.valueOf(payload.getOrDefault("Symbol", "UNKNOWN")))
                .exchange(String.valueOf(payload.getOrDefault("Exch", "")))
                .quantity(quantity)
                .averagePrice(avgPrice)
                .lastTradedPrice(lastPrice)
                .pnl(pnl)
                .dayPnl(BigDecimal.ZERO) // The API doesn't seem to provide day's PnL
                .build();
    }

    private Map<String, Object> parseJson(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            log.warn("Unable to parse FivePaisa response: {}", ex.getMessage());
            return Collections.emptyMap();
        }
    }

    private MarketFeedData mapMarketFeedFromApiResponse(Map<String, Object> payload) {
        return MarketFeedData.builder()
                .symbol(String.valueOf(payload.getOrDefault("Symbol", "UNKNOWN")))
                .lastTradedPrice(new BigDecimal(String.valueOf(payload.getOrDefault("LastRate", "0"))))
                .high(new BigDecimal(String.valueOf(payload.getOrDefault("High", "0"))))
                .low(new BigDecimal(String.valueOf(payload.getOrDefault("Low", "0"))))
                .previousClose(new BigDecimal(String.valueOf(payload.getOrDefault("PClose", "0"))))
                .changePercent(new BigDecimal(String.valueOf(payload.getOrDefault("ChgPer", "0"))))
                .volume(Long.parseLong(String.valueOf(payload.getOrDefault("TotalQty", "0"))))
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
            // We can no longer auto-authenticate. We must throw an exception.
            throw new IllegalStateException("5Paisa access token is missing or expired. Please log in again via the auth endpoints.");
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

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> target = new HashMap<>();
            map.forEach((k, v) -> target.put(String.valueOf(k), v));
            return target;
        }
        return Collections.emptyMap();
    }

    private Instant resolveExpiryInstant(Map<String, Object> bodyMap) {
        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        return ZonedDateTime.now(istZone).with(LocalTime.MAX).toInstant();
    }
}