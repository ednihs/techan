package com.stockanalyzer.service;

import com.stockanalyzer.dto.EnrichedTechnicalIndicatorDTO;
import com.stockanalyzer.dto.TechnicalIndicatorDTO;
import com.stockanalyzer.dto.MarketFeedData;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import org.springframework.data.domain.PageRequest;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class TechnicalAnalysisService {

    @PersistenceContext
    private EntityManager entityManager;

    private final PriceDataRepository priceDataRepository;
    private final TechnicalIndicatorRepository technicalIndicatorRepository;
    private final FivePaisaService fivePaisaService;
    private final Core taLib = new Core();

    @Value("${technical-analysis.indicators.lookback-days:100}")
    private int lookbackDays;

    //@Scheduled(cron = "0 32 0 * * MON-FRI", zone = "Asia/Kolkata")
    @Transactional
    public void calculateDailyIndicators() {
        calculateIndicatorsForDate(LocalDate.now());
    }

    @Transactional
    public List<TechnicalIndicator> calculateIndicatorsForDate(LocalDate asOfDate) {
        log.info("Starting technical indicator calculation for date: {}", asOfDate);

        // Check if asOfDate is today and time is before 15:30 IST
        /*if (asOfDate.equals(nowIst.toLocalDate()) && nowIst.toLocalTime().isBefore(LocalTime.of(16, 45))) {
            log.info("Market is open. Fetching live data...");
            fetchAndSaveLivePriceData(asOfDate);
        }*/

        LocalDate endDate = asOfDate.minusDays(0);
        LocalDate startDate = endDate.minusDays(5);
        double minAverageValue = 10000000.0; // 1 Crore

        List<String> symbols = priceDataRepository
                .findSymbolsByAverageValueTraded(startDate, endDate, minAverageValue);

        log.info("Found {} symbols to process for date {}", symbols.size(), asOfDate);

        List<TechnicalIndicator> indicatorsToSave = new ArrayList<>();
        List<TechnicalIndicator> allSavedIndicators = new ArrayList<>();
        final int BATCH_SIZE = 100;

        for (String symbol : symbols) {
            try {
                TechnicalIndicator indicator = calculateIndicators(symbol, asOfDate);
                if (indicator != null) {
                    indicatorsToSave.add(indicator);
                }

                if (indicatorsToSave.size() >= BATCH_SIZE) {
                    long startTime = System.currentTimeMillis();
                    List<TechnicalIndicator> savedBatch = technicalIndicatorRepository.saveAll(indicatorsToSave);
                    technicalIndicatorRepository.flush(); // Explicitly flush changes to the DB
                    allSavedIndicators.addAll(savedBatch);
                    indicatorsToSave.clear();
                    entityManager.clear(); // Detach all entities to free up memory
                    calculated += savedBatch.size();
                    System.out.println("Number of indicators updated so far: " + calculated + ", time taken for batch: " + (System.currentTimeMillis() - startTime) + "ms");
                }
            } catch (Exception e) {
                log.error("Failed to calculate indicators for symbol {} on date {}", symbol, asOfDate, e);
            }
        }

        if (!indicatorsToSave.isEmpty()) {
            long startTime = System.currentTimeMillis();
            List<TechnicalIndicator> savedBatch = technicalIndicatorRepository.saveAll(indicatorsToSave);
            allSavedIndicators.addAll(savedBatch);
            calculated += savedBatch.size();
            System.out.println("Number of indicators updated so far: " + calculated + ", time taken for final batch: " + (System.currentTimeMillis() - startTime) + "ms");
        }

        return allSavedIndicators;
    }

    private void fetchAndSaveLivePriceData(LocalDate asOfDate) {
        LocalDate endDate = asOfDate.minusDays(0);
        LocalDate startDate = endDate.minusDays(5);
        double minAverageValue = 10000000.0; // 1 Crore

        List<String> symbols = priceDataRepository
                .findSymbolsByAverageValueTraded(startDate, endDate, minAverageValue);

        if (symbols.isEmpty()) {
            log.info("No symbols found to fetch live data for.");
            return;
        }

        log.info("Fetching live data for {} symbols.", symbols.size());

        List<PriceData> priceDataToSave = new ArrayList<>();
        int batchSize = 50;
        for (int i = 0; i < symbols.size(); i += batchSize) {
            List<String> symbolBatch = symbols.subList(i, Math.min(i + batchSize, symbols.size()));
            List<FivePaisaService.MarketFeedRequestItem> requestItems = symbolBatch.stream()
                    .map(symbol -> new FivePaisaService.MarketFeedRequestItem("N", "C", symbol))
                    .collect(Collectors.toList());

            List<MarketFeedData> marketFeed = fivePaisaService.getMarketFeed(requestItems);
            for (MarketFeedData data : marketFeed) {
                if(data.getSymbol()==null || data.getSymbol().equalsIgnoreCase("null")){
                    continue;
                }
                PriceData priceData = priceDataRepository.findBySymbolAndTradeDate(data.getSymbol(), asOfDate)
                        .orElse(new PriceData());

                priceData.setSymbol(data.getSymbol());
                priceData.setTradeDate(asOfDate);
                priceData.setClosePrice(data.getLastTradedPrice());
                priceData.setHighPrice(data.getHigh());
                priceData.setLowPrice(data.getLow());
                priceData.setPrevClosePrice(data.getPreviousClose());
                priceData.setVolume(data.getVolume());
                priceData.setValueTraded(data.getLastTradedPrice().multiply(BigDecimal.valueOf(data.getVolume())).longValue());
                priceData.setNoOfTrades((int)data.getVolume());

                if (priceData.getId() == null) { // New entity
                    priceData.setOpenPrice(data.getLastTradedPrice());
                }
                priceDataToSave.add(priceData);
            }
        }

        if (!priceDataToSave.isEmpty()) {
            priceDataRepository.saveAllAndFlush(priceDataToSave);
            log.info("Saved/updated live price data for {} symbols.", priceDataToSave.size());
        }
    }

    public static int calculated = 0;
    // Transaction is now managed by the calling batch method `calculateIndicatorsForDate`
    public TechnicalIndicator calculateIndicators(String symbol, LocalDate asOfDate) {
        List<PriceData> history = priceDataRepository
                .findHistoricalData(symbol, asOfDate.minusDays(lookbackDays), asOfDate);

        return calculateIndicatorsFromHistory(history, asOfDate);
    }

    public TechnicalIndicator calculateIndicatorsFromHistory(List<PriceData> history, LocalDate asOfDate) {
        if (!ValidationUtils.hasEnoughData(history, 20)) {
            log.debug("Insufficient history for date {}", asOfDate);
            return null;
        }

        PriceData latestData = history.get(history.size() - 1);
        String symbol = latestData.getSymbol();

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
        computeIntradayFade(latestData, indicator);
        compute52WeekHighLow(symbol, asOfDate, indicator);

        getPreviousDayData(symbol, asOfDate).ifPresent(prevDayData -> {
            indicator.setPrevDayOpen(prevDayData.getOpenPrice().floatValue());
            indicator.setPrevDayHigh(prevDayData.getHighPrice().floatValue());
            indicator.setPrevDayLow(prevDayData.getLowPrice().floatValue());
            indicator.setPrevDayClose(prevDayData.getClosePrice().floatValue());

            technicalIndicatorRepository.findBySymbolAndCalculationDate(symbol, prevDayData.getTradeDate()).ifPresent(prevIndicator -> {
                indicator.setRsi14PrevDay(prevIndicator.getRsi14() != null ? prevIndicator.getRsi14().floatValue() : null);
                indicator.setMacdHistogramPrevDay(prevIndicator.getMacdHistogram() != null ? prevIndicator.getMacdHistogram().floatValue() : null);
            });

            computePivotPoints(indicator);
        });

        computeCloseVelocity5d(symbol, asOfDate, indicator);
        derivePricePositionAndMomentum(indicator);
        setDataCompleteness(indicator);
        computeDeliveryStrengthTrend(symbol, asOfDate, indicator);

        return indicator;
    }

    private void setDataCompleteness(TechnicalIndicator indicator) {
        List<Object> fields = Arrays.asList(
                indicator.getWeek52High(), indicator.getWeek52Low(), indicator.getDaysSince52wHigh(),
                indicator.getIntradayFadePct(), indicator.getCloseVelocity5d(), indicator.getPrevDayOpen(),
                indicator.getPrevDayHigh(), indicator.getPrevDayLow(), indicator.getPrevDayClose(),
                indicator.getRsi14PrevDay(), indicator.getMacdHistogramPrevDay(), indicator.getPivotPoint(),
                indicator.getResistance1(), indicator.getResistance2(), indicator.getSupport1(), indicator.getSupport2()
        );

        long nullCount = fields.stream().filter(java.util.Objects::isNull).count();

        if (nullCount == 0) {
            indicator.setDataCompleteness("FULL");
        } else if (nullCount < fields.size() / 2) {
            indicator.setDataCompleteness("PARTIAL");
        } else {
            indicator.setDataCompleteness("MINIMAL");
        }
    }

    private void computePivotPoints(TechnicalIndicator indicator) {
        if (indicator.getPrevDayHigh() == null || indicator.getPrevDayLow() == null || indicator.getPrevDayClose() == null) {
            return;
        }

        float high = indicator.getPrevDayHigh();
        float low = indicator.getPrevDayLow();
        float close = indicator.getPrevDayClose();

        float pivotPoint = (high + low + close) / 3;
        indicator.setPivotPoint(pivotPoint);

        float r1 = (pivotPoint * 2) - low;
        float r2 = pivotPoint + (high - low);
        indicator.setResistance1(r1);
        indicator.setResistance2(r2);

        float s1 = (pivotPoint * 2) - high;
        float s2 = pivotPoint - (high - low);
        indicator.setSupport1(s1);
        indicator.setSupport2(s2);
    }

    private void derivePricePositionAndMomentum(TechnicalIndicator indicator) {
        if (indicator.getCloseVelocity5d() != null) {
            // This is a simplification. A true acceleration would compare velocity over time.
            // For now, positive is accelerating, negative is decelerating.
            if (indicator.getCloseVelocity5d() > 0.1) {
                indicator.setMomentumDirection("ACCELERATING");
            } else if (indicator.getCloseVelocity5d() < -0.1) {
                indicator.setMomentumDirection("DECELERATING");
            } else {
                indicator.setMomentumDirection("STABLE");
            }
        }
    }

    private void computeCloseVelocity5d(String symbol, LocalDate asOfDate, TechnicalIndicator indicator) {
        List<PriceData> last5Days = priceDataRepository.findTop5BySymbolAndTradeDateLessThanEqualOrderByTradeDateDesc(symbol, asOfDate);
        if (last5Days.size() < 5) {
            return;
        }

        double totalPercentageChange = 0;
        for (int i = 0; i < 4; i++) {
            PriceData currentDay = last5Days.get(i);
            PriceData prevDay = last5Days.get(i + 1);
            double change = (currentDay.getClosePrice().doubleValue() - prevDay.getClosePrice().doubleValue()) / prevDay.getClosePrice().doubleValue();
            totalPercentageChange += change;
        }
        indicator.setCloseVelocity5d((float) (totalPercentageChange / 4 * 100));
    }

    private Optional<PriceData> getPreviousDayData(String symbol, LocalDate date) {
        return priceDataRepository.findTop20BySymbolAndTradeDateLessThanOrderByTradeDateDesc(symbol, date)
                .stream()
                .findFirst();
    }

    private void compute52WeekHighLow(String symbol, LocalDate asOfDate, TechnicalIndicator indicator) {
        LocalDate startDate = asOfDate.minusYears(1);
        priceDataRepository.findTopBySymbolAndTradeDateBetweenOrderByClosePriceDesc(symbol, startDate, asOfDate)
                .ifPresent(priceData -> {
                    indicator.setWeek52High(priceData.getClosePrice().floatValue());
                    indicator.setDaysSince52wHigh((int) java.time.temporal.ChronoUnit.DAYS.between(priceData.getTradeDate(), asOfDate));
                });
        priceDataRepository.findTopBySymbolAndTradeDateBetweenOrderByClosePriceAsc(symbol, startDate, asOfDate)
                .ifPresent(priceData -> indicator.setWeek52Low(priceData.getClosePrice().floatValue()));
    }

    private void computeIntradayFade(PriceData latestData, TechnicalIndicator indicator) {
        if (latestData.getHighPrice() != null && latestData.getClosePrice() != null && latestData.getClosePrice().compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal high = latestData.getHighPrice();
            BigDecimal close = latestData.getClosePrice();
            BigDecimal fade = high.subtract(close)
                    .divide(close, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            indicator.setIntradayFadePct(fade.floatValue());
        }
    }

    public TechnicalIndicator calculateTechnicalIndicators(PriceData latestPriceData, List<PriceData> history) {
        if (!ValidationUtils.hasEnoughData(history, 20)) {
            log.debug("Insufficient history for {} on {}", latestPriceData.getSymbol(), latestPriceData.getTradeDate());
            return null;
        }

        double[] close = history.stream().map(PriceData::getClosePrice).mapToDouble(BigDecimal::doubleValue).toArray();
        double[] high = history.stream().map(PriceData::getHighPrice).mapToDouble(BigDecimal::doubleValue).toArray();
        double[] low = history.stream().map(PriceData::getLowPrice).mapToDouble(BigDecimal::doubleValue).toArray();
        double[] volume = history.stream().mapToDouble(pd -> Optional.ofNullable(pd.getVolume()).orElse(0L)).toArray();

        TechnicalIndicator indicator = technicalIndicatorRepository
                .findBySymbolAndCalculationDate(latestPriceData.getSymbol(), latestPriceData.getTradeDate())
                .orElseGet(TechnicalIndicator::new);

        indicator.setSymbol(latestPriceData.getSymbol());
        indicator.setCalculationDate(latestPriceData.getTradeDate());

        computeRsi(close, indicator);
        computeEma(close, indicator);
        computeSma(close, volume, indicator);
        computeAtr(high, low, close, indicator);
        computeMacd(close, indicator);
        computeBollinger(close, indicator);
        computeVwap(history, indicator);
        computeCustomStrength(history, indicator);
        computeIntradayFade(latestPriceData, indicator);
        compute52WeekHighLow(latestPriceData.getSymbol(), latestPriceData.getTradeDate(), indicator);

        getPreviousDayData(latestPriceData.getSymbol(), latestPriceData.getTradeDate()).ifPresent(prevDayData -> {
            indicator.setPrevDayOpen(prevDayData.getOpenPrice().floatValue());
            indicator.setPrevDayHigh(prevDayData.getHighPrice().floatValue());
            indicator.setPrevDayLow(prevDayData.getLowPrice().floatValue());
            indicator.setPrevDayClose(prevDayData.getClosePrice().floatValue());

            technicalIndicatorRepository.findBySymbolAndCalculationDate(latestPriceData.getSymbol(), prevDayData.getTradeDate()).ifPresent(prevIndicator -> {
                indicator.setRsi14PrevDay(prevIndicator.getRsi14() != null ? prevIndicator.getRsi14().floatValue() : null);
                indicator.setMacdHistogramPrevDay(prevIndicator.getMacdHistogram() != null ? prevIndicator.getMacdHistogram().floatValue() : null);
            });

            computePivotPoints(indicator);
        });

        computeCloseVelocity5d(latestPriceData.getSymbol(), latestPriceData.getTradeDate(), indicator);
        derivePricePositionAndMomentum(indicator);
        setDataCompleteness(indicator);
        computeDeliveryStrengthTrend(latestPriceData.getSymbol(), latestPriceData.getTradeDate(), indicator);

        return indicator;
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

    public List<PriceData> getHistoricalPriceData(String symbol, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        return priceDataRepository.findHistoricalData(symbol, startDate, endDate);
    }

    public EnrichedTechnicalIndicatorDTO getEnrichedTechnicalIndicator(String symbol, LocalDate date) {
        Optional<TechnicalIndicator> latestIndicatorOpt = technicalIndicatorRepository.findBySymbolAndCalculationDate(symbol, date);
        if (latestIndicatorOpt.isEmpty()) {
            // If there's no indicator for the requested date, it might be a non-trading day.
            // Let's find the most recent trading day and use that instead.
            List<LocalDate> lastTradeDate = priceDataRepository.findLastNTradeDates(symbol, date, PageRequest.of(0, 1));
            if (lastTradeDate.isEmpty()) {
                return null; // No price data for this symbol at all
            }
            LocalDate mostRecentDate = lastTradeDate.get(0);
            latestIndicatorOpt = technicalIndicatorRepository.findBySymbolAndCalculationDate(symbol, mostRecentDate);
            if(latestIndicatorOpt.isEmpty()){
                return null; // No indicator for the most recent trading day either
            }
        }
        
        TechnicalIndicator latestIndicator = latestIndicatorOpt.get();
        LocalDate latestIndicatorDate = latestIndicator.getCalculationDate();

        List<LocalDate> last4TradeDates = priceDataRepository.findLastNTradeDates(symbol, latestIndicatorDate, PageRequest.of(0, 4));
        List<TechnicalIndicator> historicalIndicators = technicalIndicatorRepository.findBySymbolAndCalculationDateInOrderByCalculationDateDesc(symbol, last4TradeDates);

        PriceData priceData = priceDataRepository.findBySymbolAndTradeDate(symbol, latestIndicatorDate).orElse(new PriceData());

        EnrichedTechnicalIndicatorDTO dto = new EnrichedTechnicalIndicatorDTO();
        // Manually map fields from latestIndicator to the DTO
        dto.setSymbol(latestIndicator.getSymbol());
        dto.setCalculationDate(latestIndicator.getCalculationDate());
        dto.setCreatedAt(latestIndicator.getCreatedAt());
        dto.setRsi14(latestIndicator.getRsi14());
        dto.setEma9(latestIndicator.getEma9());
        dto.setEma21(latestIndicator.getEma21());
        dto.setSma20(latestIndicator.getSma20());
        dto.setAtr14(latestIndicator.getAtr14());
        dto.setVwap(latestIndicator.getVwap());
        dto.setVolumeRatio(latestIndicator.getVolumeRatio());
        dto.setPriceStrength(latestIndicator.getPriceStrength());
        dto.setVolumeStrength(latestIndicator.getVolumeStrength());
        dto.setDeliveryStrength(latestIndicator.getDeliveryStrength());
        dto.setMacd(latestIndicator.getMacd());
        dto.setMacdSignal(latestIndicator.getMacdSignal());
        dto.setMacdHistogram(latestIndicator.getMacdHistogram());
        dto.setBollingerUpper(latestIndicator.getBollingerUpper());
        dto.setBollingerLower(latestIndicator.getBollingerLower());
        dto.setBollingerWidth(latestIndicator.getBollingerWidth());

        dto.setWeek52High(latestIndicator.getWeek52High());
        dto.setWeek52Low(latestIndicator.getWeek52Low());
        dto.setDaysSince52wHigh(latestIndicator.getDaysSince52wHigh());
        dto.setIntradayFadePct(latestIndicator.getIntradayFadePct());
        dto.setCloseVelocity5d(latestIndicator.getCloseVelocity5d());
        dto.setPrevDayOpen(latestIndicator.getPrevDayOpen());
        dto.setPrevDayHigh(latestIndicator.getPrevDayHigh());
        dto.setPrevDayLow(latestIndicator.getPrevDayLow());
        dto.setPrevDayClose(latestIndicator.getPrevDayClose());
        dto.setRsi14PrevDay(latestIndicator.getRsi14PrevDay());
        dto.setMacdHistogramPrevDay(latestIndicator.getMacdHistogramPrevDay());
        dto.setPivotPoint(latestIndicator.getPivotPoint());
        dto.setResistance1(latestIndicator.getResistance1());
        dto.setResistance2(latestIndicator.getResistance2());
        dto.setSupport1(latestIndicator.getSupport1());
        dto.setSupport2(latestIndicator.getSupport2());
        dto.setPricePositionStage(latestIndicator.getPricePositionStage());
        dto.setMomentumDirection(latestIndicator.getMomentumDirection());
        dto.setDataCompleteness(latestIndicator.getDataCompleteness());
        dto.setDeliveryStrength5dAvg(latestIndicator.getDeliveryStrength5dAvg());
        dto.setDeliveryStrengthTrend(latestIndicator.getDeliveryStrengthTrend());

        dto.setOpen(priceData.getOpenPrice());
        dto.setHigh(priceData.getHighPrice());
        dto.setLow(priceData.getLowPrice());
        dto.setClose(priceData.getClosePrice());

        dto.setSupportLevels(calculateSupportLevels(priceData, latestIndicator));
        dto.setResistanceLevels(calculateResistanceLevels(priceData, latestIndicator));
        dto.setEarlyBirdRecommendations(detectEarlyBirdOpportunities(symbol, priceData, historicalIndicators));
        dto.setHistoricalIndicators(historicalIndicators.stream().map(this::mapToDTO).collect(Collectors.toList()));

        calculateAndSetDynamicPivotDistances(dto, priceData, latestIndicator);
        deriveDynamicPricePosition(dto);

        return dto;
    }

    public TechnicalIndicatorDTO mapToDTO(TechnicalIndicator indicator) {
        if (indicator == null) {
            return null;
        }
        return TechnicalIndicatorDTO.builder()
                .symbol(indicator.getSymbol())
                .calculationDate(indicator.getCalculationDate())
                .createdAt(indicator.getCreatedAt())
                .rsi14(indicator.getRsi14())
                .ema9(indicator.getEma9())
                .ema21(indicator.getEma21())
                .sma20(indicator.getSma20())
                .atr14(indicator.getAtr14())
                .vwap(indicator.getVwap())
                .volumeRatio(indicator.getVolumeRatio())
                .priceStrength(indicator.getPriceStrength())
                .volumeStrength(indicator.getVolumeStrength())
                .deliveryStrength(indicator.getDeliveryStrength())
                .macd(indicator.getMacd())
                .macdSignal(indicator.getMacdSignal())
                .macdHistogram(indicator.getMacdHistogram())
                .bollingerUpper(indicator.getBollingerUpper())
                .bollingerLower(indicator.getBollingerLower())
                .bollingerWidth(indicator.getBollingerWidth())
                .week52High(indicator.getWeek52High())
                .week52Low(indicator.getWeek52Low())
                .daysSince52wHigh(indicator.getDaysSince52wHigh())
                .intradayFadePct(indicator.getIntradayFadePct())
                .closeVelocity5d(indicator.getCloseVelocity5d())
                .prevDayOpen(indicator.getPrevDayOpen())
                .prevDayHigh(indicator.getPrevDayHigh())
                .prevDayLow(indicator.getPrevDayLow())
                .prevDayClose(indicator.getPrevDayClose())
                .rsi14PrevDay(indicator.getRsi14PrevDay())
                .macdHistogramPrevDay(indicator.getMacdHistogramPrevDay())
                .pivotPoint(indicator.getPivotPoint())
                .resistance1(indicator.getResistance1())
                .resistance2(indicator.getResistance2())
                .support1(indicator.getSupport1())
                .support2(indicator.getSupport2())
                .pricePositionStage(indicator.getPricePositionStage())
                .momentumDirection(indicator.getMomentumDirection())
                .dataCompleteness(indicator.getDataCompleteness())
                .deliveryStrength5dAvg(indicator.getDeliveryStrength5dAvg())
                .deliveryStrengthTrend(indicator.getDeliveryStrengthTrend())
                .build();
    }


    private Map<String, Double> calculateSupportLevels(PriceData priceData, TechnicalIndicator indicator) {
        Map<String, Double> support = new HashMap<>();
        support.put("SMA20", indicator.getSma20());
        support.put("Bollinger Lower", indicator.getBollingerLower());
        if (priceData.getPrevClosePrice() != null) {
            support.put("Previous Close", priceData.getPrevClosePrice().doubleValue());
        }
        return support;
    }

    private Map<String, Double> calculateResistanceLevels(PriceData priceData, TechnicalIndicator indicator) {
        Map<String, Double> resistance = new HashMap<>();
        resistance.put("SMA20", indicator.getSma20());
        resistance.put("Bollinger Upper", indicator.getBollingerUpper());
        if (priceData.getPrevClosePrice() != null) {
            resistance.put("Previous Close", priceData.getPrevClosePrice().doubleValue());
        }
        return resistance;
    }

    private List<String> detectEarlyBirdOpportunities(String symbol, PriceData currentPrice, List<TechnicalIndicator> historicalIndicators) {
        List<String> recommendations = new ArrayList<>();
        if (historicalIndicators.size() < 4) {
            recommendations.add("EarlyBird ΔRSI14 over last 3 sessions not computed (indicator file lacks RSI history; OHLC available but RSI history not pre-computed). EarlyBird picks therefore not selected to avoid unverifiable ΔRSI14.");
            return recommendations;
        }

        // Assuming historicalIndicators are sorted descending by date
        double rsiNow = historicalIndicators.get(0).getRsi14();
        double rsi3DaysAgo = historicalIndicators.get(3).getRsi14();
        double deltaRsi = rsiNow - rsi3DaysAgo;

        if (deltaRsi > 10) {
            recommendations.add(String.format("ΔRSI14 > 10 in last 3 sessions (%.2f)", deltaRsi));
        }

        return recommendations;
    }

    public List<EnrichedTechnicalIndicatorDTO> getEnrichedTechnicalIndicatorsForDate(LocalDate date) {
        List<String> symbols = priceDataRepository.findDistinctSymbols();
        if (symbols.isEmpty()) {
            return new ArrayList<>();
        }

        // Determine the most recent trading date and the last 4 trading dates based on a sample symbol.
        // This assumes that all symbols have similar trading schedules.
        List<LocalDate> lastTradeDate = priceDataRepository.findLastNTradeDates(symbols.get(0), date, PageRequest.of(0, 1));
        if (lastTradeDate.isEmpty()) {
            return new ArrayList<>();
        }
        LocalDate mostRecentDate = lastTradeDate.get(0);
        List<LocalDate> last4TradeDates = priceDataRepository.findLastNTradeDates(symbols.get(0), mostRecentDate, PageRequest.of(0, 4));

        // Fetch all required data in bulk
        List<TechnicalIndicator> allIndicators = technicalIndicatorRepository.findBySymbolInAndCalculationDateInOrderByCalculationDateDesc(symbols, last4TradeDates);
        List<PriceData> allPriceData = priceDataRepository.findBySymbolInAndTradeDate(symbols, mostRecentDate);

        // Group data by symbol for efficient processing
        Map<String, List<TechnicalIndicator>> indicatorsBySymbol = allIndicators.stream().collect(Collectors.groupingBy(TechnicalIndicator::getSymbol));
        Map<String, PriceData> priceDataBySymbol = allPriceData.stream().collect(Collectors.toMap(PriceData::getSymbol, Function.identity()));

        // Build the DTOs
        return symbols.stream()
                .map(symbol -> {
                    List<TechnicalIndicator> historicalIndicators = indicatorsBySymbol.get(symbol);
                    PriceData priceData = priceDataBySymbol.get(symbol);

                    if (historicalIndicators == null || historicalIndicators.isEmpty() || priceData == null) {
                        return null;
                    }

                    TechnicalIndicator latestIndicator = historicalIndicators.get(0);

                    EnrichedTechnicalIndicatorDTO dto = new EnrichedTechnicalIndicatorDTO();
                    dto.setSymbol(latestIndicator.getSymbol());
                    dto.setRsi14(latestIndicator.getRsi14());
                    dto.setEma9(latestIndicator.getEma9());
                    dto.setEma21(latestIndicator.getEma21());
                    dto.setSma20(latestIndicator.getSma20());
                    dto.setAtr14(latestIndicator.getAtr14());
                    dto.setVwap(latestIndicator.getVwap());
                    dto.setVolumeRatio(latestIndicator.getVolumeRatio());
                    dto.setPriceStrength(latestIndicator.getPriceStrength());
                    dto.setVolumeStrength(latestIndicator.getVolumeStrength());
                    dto.setDeliveryStrength(latestIndicator.getDeliveryStrength());
                    dto.setMacd(latestIndicator.getMacd());
                    dto.setMacdSignal(latestIndicator.getMacdSignal());
                    dto.setMacdHistogram(latestIndicator.getMacdHistogram());
                    dto.setBollingerUpper(latestIndicator.getBollingerUpper());
                    dto.setBollingerLower(latestIndicator.getBollingerLower());
                    dto.setBollingerWidth(latestIndicator.getBollingerWidth());

                    // Manually map new fields
                    dto.setWeek52High(latestIndicator.getWeek52High());
                    dto.setWeek52Low(latestIndicator.getWeek52Low());
                    dto.setDaysSince52wHigh(latestIndicator.getDaysSince52wHigh());
                    dto.setIntradayFadePct(latestIndicator.getIntradayFadePct());
                    dto.setCloseVelocity5d(latestIndicator.getCloseVelocity5d());
                    dto.setPrevDayOpen(latestIndicator.getPrevDayOpen());
                    dto.setPrevDayHigh(latestIndicator.getPrevDayHigh());
                    dto.setPrevDayLow(latestIndicator.getPrevDayLow());
                    dto.setPrevDayClose(latestIndicator.getPrevDayClose());
                    dto.setRsi14PrevDay(latestIndicator.getRsi14PrevDay());
                    dto.setMacdHistogramPrevDay(latestIndicator.getMacdHistogramPrevDay());
                    dto.setPivotPoint(latestIndicator.getPivotPoint());
                    dto.setResistance1(latestIndicator.getResistance1());
                    dto.setResistance2(latestIndicator.getResistance2());
                    dto.setSupport1(latestIndicator.getSupport1());
                    dto.setSupport2(latestIndicator.getSupport2());
                    dto.setPricePositionStage(latestIndicator.getPricePositionStage());
                    dto.setMomentumDirection(latestIndicator.getMomentumDirection());
                    dto.setDataCompleteness(latestIndicator.getDataCompleteness());
                    dto.setDeliveryStrength5dAvg(latestIndicator.getDeliveryStrength5dAvg());
                    dto.setDeliveryStrengthTrend(latestIndicator.getDeliveryStrengthTrend());

                    dto.setOpen(priceData.getOpenPrice());
                    dto.setHigh(priceData.getHighPrice());
                    dto.setLow(priceData.getLowPrice());
                    dto.setClose(priceData.getClosePrice());

                    calculateAndSetDynamicPivotDistances(dto, priceData, latestIndicator);
                    deriveDynamicPricePosition(dto);

                    return dto;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void calculateAndSetDynamicPivotDistances(EnrichedTechnicalIndicatorDTO dto, PriceData priceData, TechnicalIndicator indicator) {
        if (indicator.getPivotPoint() == null || priceData.getClosePrice() == null || priceData.getClosePrice().floatValue() == 0) {
            return;
        }

        float close = priceData.getClosePrice().floatValue();
        float pivot = indicator.getPivotPoint();

        dto.setPctFromPivot((close - pivot) / pivot * 100);

        if (indicator.getResistance1() != null) {
            dto.setPctToResistance1((indicator.getResistance1() - close) / close * 100);
        }
        if (indicator.getResistance2() != null) {
            dto.setPctToResistance2((indicator.getResistance2() - close) / close * 100);
        }
        if (indicator.getSupport1() != null) {
            dto.setPctToSupport1((indicator.getSupport1() - close) / close * 100);
        }
        if (indicator.getSupport2() != null) {
            dto.setPctToSupport2((close - indicator.getSupport2()) / close * 100);
        }
    }

    private void deriveDynamicPricePosition(EnrichedTechnicalIndicatorDTO dto) {
        if (dto.getPctFromPivot() != null) {
            float pctFromPivot = dto.getPctFromPivot();
            if (pctFromPivot < -1) {
                dto.setPricePositionStage("BELOW_PIVOT");
            } else if (pctFromPivot >= -1 && pctFromPivot <= 1) {
                dto.setPricePositionStage("AT_PIVOT");
            } else if (pctFromPivot > 1 && pctFromPivot <= 3) {
                dto.setPricePositionStage("EARLY_STAGE");
            } else if (pctFromPivot > 3 && pctFromPivot <= 5) {
                dto.setPricePositionStage("MID_STAGE");
            } else {
                dto.setPricePositionStage("LATE_STAGE");
            }
        }
    }

    private void computeDeliveryStrengthTrend(String symbol, LocalDate asOfDate, TechnicalIndicator indicator) {
        List<TechnicalIndicator> last5DaysIndicators = technicalIndicatorRepository.findTop5BySymbolAndCalculationDateLessThanEqualOrderByCalculationDateDesc(symbol, asOfDate);

        if (last5DaysIndicators.size() < 2) { // Need at least 2 days to determine a trend
            indicator.setDeliveryStrengthTrend("STABLE");
            return;
        }

        double average = last5DaysIndicators.stream()
                .map(TechnicalIndicator::getDeliveryStrength)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        indicator.setDeliveryStrength5dAvg((float) average);

        Double latestStrength = last5DaysIndicators.get(0).getDeliveryStrength();
        Double previousStrength = last5DaysIndicators.get(1).getDeliveryStrength();

        if (latestStrength != null && previousStrength != null) {
            if (latestStrength > previousStrength) {
                indicator.setDeliveryStrengthTrend("INCREASING");
            } else if (latestStrength < previousStrength) {
                indicator.setDeliveryStrengthTrend("DECREASING");
            } else {
                indicator.setDeliveryStrengthTrend("STABLE");
            }
        } else {
            indicator.setDeliveryStrengthTrend("STABLE");
        }
    }

    @Transactional
    public void scheduledFetchAndSaveLivePriceData() {
        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        ZonedDateTime nowIst = ZonedDateTime.now(istZone);

        // Run only during market hours (9:15 AM to 3:30 PM IST)
       // if ( fo("Running scheduled job to fetch and save live price data.");
       log.info("Running scheduled job to fetch and save live price data.");     
       fetchAndSaveLivePriceData(nowIst.toLocalDate());
       // }
    }
}
