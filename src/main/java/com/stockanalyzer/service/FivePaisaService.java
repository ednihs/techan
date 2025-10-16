package com.stockanalyzer.service;

import com.FivePaisa.api.RestClient;
import com.FivePaisa.config.AppConfig;
import com.FivePaisa.service.Properties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FivePaisaService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private final WebClient.Builder webClientBuilder;

    @Value("${fivepaisa.api.base-url:https://openapi.5paisa.com/VendorsAPI/Service1.svc}")
    private String baseUrl;

    @Value("${fivepaisa.api.client-code:demo}")
    private String clientCode;

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

    @Value("${fivepaisa.api.pin:}")
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

    @Value("${fivepaisa.api.app-source:11033}")
    private int appSource;

    private WebClient webClient;

    private final AtomicReference<String> bearerToken = new AtomicReference<>("");
    private final AtomicReference<String> refreshToken = new AtomicReference<>("");
    private final AtomicReference<String> feedToken = new AtomicReference<>("");
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
            if (!hasTotpPrerequisites()) {
                log.warn("Skipping FivePaisa authentication because mandatory credentials are missing (appName/userKey/userId/password/pin/totp)");
                return;
            }

            try {
                String totp = resolveTotpCode();
                if (!StringUtils.hasText(totp)) {
                    log.warn("Unable to generate FivePaisa TOTP code. Provide either fivepaisa.api.totp-secret or fivepaisa.api.totp-code");
                    return;
                }

                AppConfig config = buildAppConfig();
                Properties properties = buildProperties();

                RestClient restClient = new RestClient(config, properties);
                String responseJson = restClient.getTotpSession(properties.getClientcode(), totp, tradingPin);

                Map<String, Object> response = parseJson(responseJson);
                Map<String, Object> bodyMap = asMap(response.get("body"));
                if (bodyMap.isEmpty()) {
                    log.warn("FivePaisa authentication response did not include a body payload");
                    return;
                }

                long status = parseLong(bodyMap.get("Status"), -1L);
                if (status != 0L) {
                    log.warn("FivePaisa authentication failed for client {}: status={} message={}", clientCode, status, extractString(bodyMap.get("Message")));
                    return;
                }

                String accessToken = extractString(bodyMap.get("AccessToken"));
                if (!StringUtils.hasText(accessToken)) {
                    log.warn("FivePaisa authentication response did not include an access token");
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

    private boolean hasTotpPrerequisites() {
        return StringUtils.hasText(appName)
                && StringUtils.hasText(appVersion)
                && StringUtils.hasText(osName)
                && StringUtils.hasText(encryptKey)
                && StringUtils.hasText(userKey)
                && StringUtils.hasText(userId)
                && StringUtils.hasText(password)
                && StringUtils.hasText(loginId)
                && StringUtils.hasText(tradingPin)
                && StringUtils.hasText(clientCode)
                && (StringUtils.hasText(totpSecret) || StringUtils.hasText(staticTotpCode));
    }

    private AppConfig buildAppConfig() {
        AppConfig config = new AppConfig();
        config.setAppName(appName);
        config.setAppVer(appVersion);
        config.setOsName(osName);
        config.setEncryptKey(encryptKey);
        config.setKey(userKey);
        config.setUserId(userId);
        config.setPassword(password);
        config.setLoginId(loginId);
        return config;
    }

    private Properties buildProperties() {
        Properties properties = new Properties();
        properties.setClientcode(clientCode);
        properties.setAppSource(appSource);
        if (StringUtils.hasText(deviceIp)) {
            properties.setRemoteIpAddress(deviceIp);
        }
        if (StringUtils.hasText(deviceId)) {
            properties.setMachineID(deviceId);
        }
        return properties;
    }

    private String resolveTotpCode() {
        if (StringUtils.hasText(totpSecret)) {
            try {
                return generateTotp(totpSecret, totpDigits, totpStepSeconds);
            } catch (Exception ex) {
                log.warn("Failed to generate FivePaisa TOTP using provided secret: {}", ex.getMessage());
            }
        }
        if (StringUtils.hasText(staticTotpCode)) {
            return staticTotpCode.trim();
        }
        return "";
    }

    private String generateTotp(String secret, int digits, int stepSeconds) throws Exception {
        byte[] key = decodeBase32(secret);
        if (key.length == 0) {
            throw new IllegalArgumentException("Empty Base32 secret");
        }
        int effectiveDigits = Math.max(1, Math.min(8, digits));
        int effectiveStep = Math.max(1, stepSeconds);
        long timeWindow = Instant.now().getEpochSecond() / effectiveStep;
        byte[] data = ByteBuffer.allocate(8).putLong(timeWindow).array();

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(data);

        int offset = hash[hash.length - 1] & 0x0F;
        if (offset + 4 > hash.length) {
            throw new IllegalStateException("Unexpected hash length for TOTP generation");
        }

        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);

        int modulus = 1;
        for (int i = 0; i < effectiveDigits; i++) {
            modulus *= 10;
        }

        int otp = binary % modulus;
        return String.format(Locale.US, "%0" + effectiveDigits + "d", otp);
    }

    private byte[] decodeBase32(String secret) {
        String normalized = secret == null ? "" : secret.replace(" ", "").replace("=", "").toUpperCase(Locale.US);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int buffer = 0;
        int bitsLeft = 0;
        for (char ch : normalized.toCharArray()) {
            int value = BASE32_ALPHABET.indexOf(ch);
            if (value < 0) {
                throw new IllegalArgumentException("Invalid Base32 character: " + ch);
            }
            buffer = (buffer << 5) | value;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output.write((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return output.toByteArray();
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
