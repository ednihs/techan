package com.stockanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class StockAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockAnalyzerApplication.class, args);
    }
}
