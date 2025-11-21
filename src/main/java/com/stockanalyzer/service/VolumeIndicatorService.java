package com.stockanalyzer.service;

import com.stockanalyzer.entity.CrudeOHLCVData;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for calculating volume-based technical indicators for crude oil analysis.
 */
@Service
@Slf4j
public class VolumeIndicatorService {

    private final Core taLib = new Core();

    /**
     * Calculate On-Balance Volume (OBV)
     * Formula: If close > prev_close: OBV = prev_OBV + volume
     *          If close < prev_close: OBV = prev_OBV - volume
     *          If close = prev_close: OBV = prev_OBV
     */
    public long calculateOBV(List<CrudeOHLCVData> candles, int index, Long previousOBV) {
        if (index == 0) {
            return candles.get(0).getVolume();
        }

        BigDecimal currentClose = candles.get(index).getClose();
        BigDecimal previousClose = candles.get(index - 1).getClose();
        long currentVolume = candles.get(index).getVolume();
        long prevOBV = previousOBV != null ? previousOBV : 0L;

        if (currentClose.compareTo(previousClose) > 0) {
            return prevOBV + currentVolume;
        } else if (currentClose.compareTo(previousClose) < 0) {
            return prevOBV - currentVolume;
        } else {
            return prevOBV;
        }
    }

    /**
     * Calculate EMA of OBV for smoothing
     * Uses 10-period EMA by default
     */
    public BigDecimal calculateOBVEMA(List<Long> obvValues, int period) {
        if (obvValues.isEmpty()) {
            return BigDecimal.ZERO;
        }

        double[] obvArray = obvValues.stream()
                .mapToDouble(Long::doubleValue)
                .toArray();

        double[] output = new double[obvArray.length];
        MInteger outBeg = new MInteger();
        MInteger outNb = new MInteger();

        RetCode code = taLib.ema(0, obvArray.length - 1, obvArray, period, outBeg, outNb, output);

        if (code == RetCode.Success && outNb.value > 0) {
            return BigDecimal.valueOf(output[outNb.value - 1]).setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Calculate Volume Weighted Average Price (VWAP)
     * Formula: VWAP = Σ(Typical Price × Volume) / Σ(Volume)
     * Typical Price = (High + Low + Close) / 3
     * 
     * For intraday timeframes, VWAP is reset at the start of each trading day
     */
    public BigDecimal calculateVWAP(List<CrudeOHLCVData> candles, int index, boolean resetDaily) {
        int startIdx = 0;

        if (resetDaily && index > 0) {
            // Find the start of the current day
            LocalDate currentDate = candles.get(index).getTimestamp().toLocalDate();
            for (int i = index; i >= 0; i--) {
                LocalDate candleDate = candles.get(i).getTimestamp().toLocalDate();
                if (!candleDate.equals(currentDate)) {
                    startIdx = i + 1;
                    break;
                }
            }
        }

        BigDecimal cumulativeTPV = BigDecimal.ZERO;
        long cumulativeVolume = 0;

        for (int i = startIdx; i <= index; i++) {
            CrudeOHLCVData candle = candles.get(i);
            BigDecimal typicalPrice = candle.getHigh()
                    .add(candle.getLow())
                    .add(candle.getClose())
                    .divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP);

            cumulativeTPV = cumulativeTPV.add(typicalPrice.multiply(BigDecimal.valueOf(candle.getVolume())));
            cumulativeVolume += candle.getVolume();
        }

        if (cumulativeVolume == 0) {
            return BigDecimal.ZERO;
        }

        return cumulativeTPV.divide(BigDecimal.valueOf(cumulativeVolume), 4, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Volume Simple Moving Average
     */
    public long calculateVolumeSMA(List<CrudeOHLCVData> candles, int index, int period) {
        if (index < period - 1) {
            return 0L;
        }

        long sum = 0;
        for (int i = index - period + 1; i <= index; i++) {
            sum += candles.get(i).getVolume();
        }

        return sum / period;
    }

    /**
     * Calculate Volume Ratio
     * Formula: (Current Volume / Volume_SMA) × 100
     * > 100% = Above average volume
     * < 100% = Below average volume
     */
    public BigDecimal calculateVolumeRatio(long currentVolume, long volumeSMA) {
        if (volumeSMA == 0) {
            return BigDecimal.valueOf(100);
        }

        return BigDecimal.valueOf(currentVolume)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(volumeSMA), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Price Volume Trend (PVT)
     * Formula: PVT = prev_PVT + [(close - prev_close) / prev_close] × volume
     */
    public BigDecimal calculatePriceVolumeTrend(List<CrudeOHLCVData> candles, int index, BigDecimal previousPVT) {
        if (index == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal currentClose = candles.get(index).getClose();
        BigDecimal previousClose = candles.get(index - 1).getClose();
        long currentVolume = candles.get(index).getVolume();
        BigDecimal prevPVT = previousPVT != null ? previousPVT : BigDecimal.ZERO;

        if (previousClose.compareTo(BigDecimal.ZERO) == 0) {
            return prevPVT;
        }

        BigDecimal priceChangePct = currentClose.subtract(previousClose)
                .divide(previousClose, 6, RoundingMode.HALF_UP);

        BigDecimal pvtChange = priceChangePct.multiply(BigDecimal.valueOf(currentVolume));

        return prevPVT.add(pvtChange).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Volume Rate of Change (Volume ROC)
     * Formula: [(current_volume - prev_volume) / prev_volume] × 100
     */
    public BigDecimal calculateVolumeRateOfChange(List<CrudeOHLCVData> candles, int index) {
        if (index == 0) {
            return BigDecimal.ZERO;
        }

        long currentVolume = candles.get(index).getVolume();
        long previousVolume = candles.get(index - 1).getVolume();

        if (previousVolume == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(currentVolume - previousVolume)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previousVolume), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate all SMAs for a given set of close prices
     */
    public BigDecimal[] calculateMultipleSMAs(double[] closePrices, int[] periods) {
        BigDecimal[] results = new BigDecimal[periods.length];

        for (int i = 0; i < periods.length; i++) {
            int period = periods[i];
            double[] output = new double[closePrices.length];
            MInteger outBeg = new MInteger();
            MInteger outNb = new MInteger();

            RetCode code = taLib.sma(0, closePrices.length - 1, closePrices, period, outBeg, outNb, output);

            if (code == RetCode.Success && outNb.value > 0) {
                results[i] = BigDecimal.valueOf(output[outNb.value - 1]).setScale(4, RoundingMode.HALF_UP);
            } else {
                results[i] = null;
            }
        }

        return results;
    }

    /**
     * Calculate price vs SMA percentage
     */
    public String calculatePriceVsSMA(BigDecimal currentPrice, BigDecimal smaValue) {
        if (smaValue == null || smaValue.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal percentage = currentPrice.subtract(smaValue)
                .divide(smaValue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        String sign = percentage.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + percentage.setScale(2, RoundingMode.HALF_UP) + "%";
    }

    /**
     * Find highest high in last N periods
     */
    public BigDecimal findHighest(List<CrudeOHLCVData> candles, int index, int periods) {
        int startIdx = Math.max(0, index - periods + 1);
        BigDecimal highest = candles.get(startIdx).getHigh();

        for (int i = startIdx + 1; i <= index; i++) {
            BigDecimal high = candles.get(i).getHigh();
            if (high.compareTo(highest) > 0) {
                highest = high;
            }
        }

        return highest;
    }

    /**
     * Find lowest low in last N periods
     */
    public BigDecimal findLowest(List<CrudeOHLCVData> candles, int index, int periods) {
        int startIdx = Math.max(0, index - periods + 1);
        BigDecimal lowest = candles.get(startIdx).getLow();

        for (int i = startIdx + 1; i <= index; i++) {
            BigDecimal low = candles.get(i).getLow();
            if (low.compareTo(lowest) < 0) {
                lowest = low;
            }
        }

        return lowest;
    }
}

