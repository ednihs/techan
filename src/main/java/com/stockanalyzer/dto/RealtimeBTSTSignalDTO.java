package com.stockanalyzer.dto;

import lombok.Data;

import java.time.LocalTime;

@Data
public class RealtimeBTSTSignalDTO {
    private String symbol;
    private String sector;
    private LocalTime signalTime;
    private double currentPrice;
    private double retailIntensity;
    private double cumulativeDelta;
    private boolean absorptionQuality;
    private boolean supplyExhaustion;
    private double weakHandsScore;
    private double entryPrice;
    private double targetPrice;
    private double stopLoss;
    private double riskRewardRatio;
    private double positionSizePercent;
    private String liquidityRisk;
    private String gapRisk;
    private int confidenceScore;
}
