package com.stockanalyzer.service;

import com.stockanalyzer.config.FivePaisaConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class FivePaisaService {

    private final WebClient webClient;
    private final FivePaisaConfig fivePaisaConfig;

    public FivePaisaService(WebClient.Builder webClientBuilder, FivePaisaConfig fivePaisaConfig) {
        this.webClient = webClientBuilder.baseUrl(fivePaisaConfig.getBaseUrl()).build();
        this.fivePaisaConfig = fivePaisaConfig;
    }

    // Methods for 5paisa API integration will be implemented here
}
