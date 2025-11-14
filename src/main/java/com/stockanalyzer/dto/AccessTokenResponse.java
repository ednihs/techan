package com.stockanalyzer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AccessTokenResponse {

    @JsonProperty("body")
    private Body body;

    @JsonProperty("head")
    private Head head;

    @Data
    public static class Body {
        @JsonProperty("AccessToken")
        private String accessToken;
        @JsonProperty("ClientCode")
        private String clientCode;
        @JsonProperty("Email_ID")
        private String emailId;
        @JsonProperty("Message")
        private String message;
        @JsonProperty("Status")
        private int status;
    }

    @Data
    public static class Head {
        @JsonProperty("responseCode")
        private String responseCode;
        @JsonProperty("status")
        private String status;
        @JsonProperty("statusDescription")
        private String statusDescription;
    }
}
