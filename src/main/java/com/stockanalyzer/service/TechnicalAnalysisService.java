package com.stockanalyzer.service;

import com.stockanalyzer.entity.PriceData;
import com.stockanalyzer.entity.TechnicalIndicator;
import com.stockanalyzer.repository.PriceDataRepository;
import com.stockanalyzer.repository.TechnicalIndicatorRepository;
import com.stockanalyzer.util.ValidationUtils;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MAType;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TechnicalAnalysisService {

    private final PriceDataRepository priceDataRepository;
    private final TechnicalIndicatorRepository technicalIndicatorRepository;
    private final Core taLib = new Core();

    @Value("${technical-analysis.indicators.lookback-days:100}")
    private int lookbackDays;

    @Scheduled(cron = "0 32 0 * * MON-FRI", zone = "Asia/Kolkata")
    @Transactional
    public void calculateDailyIndicators() {
        calculateIndicatorsForDate(LocalDate.now());
    }

    @Transactional
    public List<TechnicalIndicator> calculateIndicatorsForDate(LocalDate asOfDate) {
        log.info("Starting technical indicator calculation for date: {}", asOfDate);
        LocalDate endDate = asOfDate.minusDays(1);
        LocalDate startDate = endDate.minusDays(5);
        double minAverageValue = 10000000.0; // 1 Crore

        List<String> symbols = priceDataRepository
                .findSymbolsByAverageValueTraded(startDate, endDate, minAverageValue);

        log.info("Found {} symbols to process for date {}", symbols.size(), asOfDate);

        return symbols.stream()
                .map(symbol -> {
                    try {
                        return calculateIndicators(symbol, asOfDate);
                    } catch (Exception e) {
                        log.error("Failed to calculate indicators for symbol {} on date {}", symbol, asOfDate, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
public static int calculated = 0;
    @Transactional
    public TechnicalIndicator calculateIndicators(String symbol, LocalDate asOfDate) {
        long startTime = System.currentTimeMillis();
        List<PriceData> history = priceDataRepository
                .findHistoricalData(symbol, asOfDate.minusDays(lookbackDays), asOfDate);
        if (!ValidationUtils.hasEnoughData(history, 20)) {
            log.debug("Insufficient history for {} on {}", symbol, asOfDate);
            return null;
        }
        double[] close = history.stream().map(PriceData::getClosePrice).mapToDouble(BigDecimal::doubleValue).toArray();
        double[] high = history.stream().map(PriceData::getHighPrice).mapToDouble(BigDecimal::doubleValue).toArray();
        double[] low = history.stream().map(PriceData::getLowPrice).mapToDouble(BigDecimal::doubleValue).toArray();
        double[] volume = history.stream().mapToDouble(pd -> Optional.ofNullable(pd.getVolume()).orElse(0L)).toArray();

        TechnicalIndicator indicator = technicalIndicatorRepository
                .findBySymbolAndCalculationDate(symbol, asOfDate)
                .orElseGet(TechnicalIndicator::new);
        indicator.setSymbol(symbol);
        indicator.setCalculationDate(asOfDate);

        computeRsi(close, indicator);
        computeEma(close, indicator);
        computeSma(close, volume, indicator);
        computeAtr(high, low, close, indicator);
        computeMacd(close, indicator);
        computeBollinger(close, indicator);
        computeVwap(history, indicator);
        computeCustomStrength(history, indicator);
        System.out.println("number of indicators updated so far "+(calculated++) +" and time taken"+ (System.currentTimeMillis() - startTime) );

        return technicalIndicatorRepository.save(indicator);
    }

    private void computeRsi(double[] close, TechnicalIndicator indicator) {
        double[] output = new double[close.length];
        MInteger outBeg = new MInteger();
        MInteger outNb = new MInteger();
        RetCode code = taLib.rsi(0, close.length - 1, close, 14, outBeg, outNb, output);
        if (code == RetCode.Success && outNb.value > 0) {
            indicator.setRsi14(output[outNb.value - 1]);
        }
    }

    private void computeEma(double[] close, TechnicalIndicator indicator) {
        double[] ema9 = new double[close.length];
        double[] ema21 = new double[close.length];
        MInteger outBeg = new MInteger();
        MInteger outNb = new MInteger();
        if (taLib.ema(0, close.length - 1, close, 9, outBeg, outNb, ema9) == RetCode.Success && outNb.value > 0) {
            indicator.setEma9(ema9[outNb.value - 1]);
        }
        if (taLib.ema(0, close.length - 1, close, 21, outBeg, outNb, ema21) == RetCode.Success && outNb.value > 0) {
            indicator.setEma21(ema21[outNb.value - 1]);
        }
    }

    private void computeSma(double[] close, double[] volume, TechnicalIndicator indicator) {
        double[] sma20 = new double[close.length];
        double[] volumeSma20 = new double[volume.length];
        MInteger outBeg = new MInteger();
        MInteger outNb = new MInteger();
        if (taLib.sma(0, close.length - 1, close, 20, outBeg, outNb, sma20) == RetCode.Success && outNb.value > 0) {
            indicator.setSma20(sma20[outNb.value - 1]);
        }
        if (taLib.sma(0, volume.length - 1, volume, 20, outBeg, outNb, volumeSma20) == RetCode.Success && outNb.value > 0) {
            double currentVolume = volume[volume.length - 1];
            double averageVolume = volumeSma20[outNb.value - 1];
            indicator.setVolumeSma20(averageVolume);
            indicator.setVolumeRatio(averageVolume == 0 ? 0 : currentVolume / averageVolume);
        }
    }

    private void computeAtr(double[] high, double[] low, double[] close, TechnicalIndicator indicator) {
        double[] atr = new double[close.length];
        MInteger outBeg = new MInteger();
        MInteger outNb = new MInteger();
        if (taLib.atr(0, close.length - 1, high, low, close, 14, outBeg, outNb, atr) == RetCode.Success && outNb.value > 0) {
            indicator.setAtr14(atr[outNb.value - 1]);
        }
    }
    private void computeMacd(double[] close, TechnicalIndicator ti) {
        double[] macd = new double[close.length];
        double[] signal = new double[close.length];
        double[] hist = new double[close.length];
        MInteger beg = new MInteger(), nb = new MInteger();
        // Standard MACD(12,26,9)
        if (taLib.macd(0, close.length - 1, close, 12, 26, 9, beg, nb, macd, signal, hist) == RetCode.Success && nb.value > 0) {
            int i = nb.value - 1;
            ti.setMacd(macd[i]);
            ti.setMacdSignal(signal[i]);
            ti.setMacdHistogram(hist[i]);
        }
    }

    private void computeBollinger(double[] close, TechnicalIndicator ti) {
        double[] upper = new double[close.length];
        double[] middle = new double[close.length];
        double[] lower = new double[close.length];
        MInteger beg = new MInteger(), nb = new MInteger();
        if (taLib.bbands(0, close.length - 1, close, 20, 2.0, 2.0, MAType.Sma, beg, nb, upper, middle, lower) == RetCode.Success && nb.value > 0) {
            int i = nb.value - 1;
            ti.setBollingerUpper(upper[i]);
            ti.setBollingerLower(lower[i]);
            ti.setBollingerWidth((upper[i] - lower[i]) / middle[i]); // normalized width
        }
    }

    private void computeVwap(List<PriceData> hist, TechnicalIndicator ti) {
        // If you have intraday candles, compute true session VWAP (preferred). If only EOD, use proxy: typical price * volume / volume
        PriceData last = hist.get(hist.size() - 1);
        // Typical price
        double tp = (last.getHighPrice().doubleValue() + last.getLowPrice().doubleValue() + last.getClosePrice().doubleValue()) / 3.0;
        // Proxy daily VWAP ~ TP (use actual intraday VWAP if available)
        ti.setVwap(tp);
    }

    private void computeCustomStrength(List<PriceData> data, TechnicalIndicator indicator) {
        if (!ValidationUtils.hasEnoughData(data, 2)) {
            return;
        }
        PriceData latest = data.get(data.size() - 1);
        PriceData previous = data.get(data.size() - 2);
        double priceChange = latest.getClosePrice().subtract(previous.getClosePrice())
                .divide(previous.getClosePrice(), 4, java.math.RoundingMode.HALF_UP).doubleValue();
        double range = latest.getHighPrice().subtract(latest.getLowPrice()).doubleValue();
        double body = latest.getClosePrice().subtract(latest.getLowPrice()).doubleValue();
        double rangeRatio = range == 0 ? 0 : body / range;
        indicator.setPriceStrength((priceChange * 100) + (rangeRatio * 50));
        double averageVolume = data.stream().mapToLong(PriceData::getVolume).average().orElse(0);
        indicator.setVolumeStrength(averageVolume == 0 ? 0 : (double) latest.getVolume() / averageVolume * 100);
        indicator.setDeliveryStrength(indicator.getVolumeStrength());
    }

    public TechnicalIndicator latestIndicator(String symbol, LocalDate asOfDate) {
        return technicalIndicatorRepository.findBySymbolAndCalculationDate(symbol, asOfDate).orElse(null);
    }

    public List<TechnicalIndicator> latestIndicators(LocalDate asOfDate) {
        return technicalIndicatorRepository.findAll();
    }
}
