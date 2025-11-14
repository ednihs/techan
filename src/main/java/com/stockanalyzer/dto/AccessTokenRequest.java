package com.stockanalyzer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class AccessTokenRequest {

    @JsonProperty("head")
    private Head head;

    @JsonProperty("body")
    private Body body;

    @Data
    @AllArgsConstructor
    public static class Head {
        @JsonProperty("Key")
        private String key;
    }

    @Data
    @AllArgsConstructor
    public static class Body {
        @JsonProperty("RequestToken")
        private String requestToken;
        @JsonProperty("EncryKey")
        private String encryKey;
        @JsonProperty("UserId")
        private String userId;
    }
}
