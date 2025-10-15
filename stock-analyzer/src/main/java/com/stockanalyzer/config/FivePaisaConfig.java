package com.stockanalyzer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FivePaisaConfig {

    @Value("${fivepaisa.api.base-url}")
    private String baseUrl;

    @Value("${fivepaisa.api.app-name}")
    private String appName;

    @Value("${fivepaisa.api.app-version}")
    private String appVersion;

    @Value("${fivepaisa.api.os-name}")
    private String osName;

    @Value("${fivepaisa.api.encrypt-key}")
    private String encryptKey;

    @Value("${fivepaisa.api.user-key}")
    private String userKey;

    @Value("${fivepaisa.api.user-id}")
    private String userId;

    @Value("${fivepaisa.api.password}")
    private String password;

    @Value("${fivepaisa.api.login-id}")
    private String loginId;

    @Value("${fivepaisa.api.client-code}")
    private String clientCode;

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getOsName() {
        return osName;
    }

    public String getEncryptKey() {
        return encryptKey;
    }

    public String getUserKey() {
        return userKey;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getClientCode() {
        return clientCode;
    }
}
