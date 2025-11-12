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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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

    private final PriceDataRepository priceDataRepository;
    private final TechnicalIndicatorRepository technicalIndicatorRepository;
    private final FivePaisaService fivePaisaService;
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

        // Check if asOfDate is today and time is before 15:30 IST
        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        ZonedDateTime nowIst = ZonedDateTime.now(istZone);
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
    @Transactional
    public TechnicalIndicator calculateIndicators(String symbol, LocalDate asOfDate) {
        long startTime = System.currentTimeMillis();
        List<PriceData> history = priceDataRepository
                .findHistoricalData(symbol, asOfDate.minusDays(lookbackDays), asOfDate);

        TechnicalIndicator indicator = calculateIndicatorsFromHistory(history, asOfDate);

        if (indicator != null) {
            System.out.println("number of indicators updated so far "+(calculated++) +" and time taken"+ (System.currentTimeMillis() - startTime) );
            return technicalIndicatorRepository.save(indicator);
        }
        return null;
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

        return indicator;
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

        TechnicalIndicator indicator = new TechnicalIndicator();
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

        dto.setOpen(priceData.getOpenPrice());
        dto.setHigh(priceData.getHighPrice());
        dto.setLow(priceData.getLowPrice());
        dto.setClose(priceData.getClosePrice());

        dto.setSupportLevels(calculateSupportLevels(priceData, latestIndicator));
        dto.setResistanceLevels(calculateResistanceLevels(priceData, latestIndicator));
        dto.setEarlyBirdRecommendations(detectEarlyBirdOpportunities(symbol, priceData, historicalIndicators));
        dto.setHistoricalIndicators(historicalIndicators.stream().map(this::mapToDTO).collect(Collectors.toList()));

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

                    dto.setOpen(priceData.getOpenPrice());
                    dto.setHigh(priceData.getHighPrice());
                    dto.setLow(priceData.getLowPrice());
                    dto.setClose(priceData.getClosePrice());

                    return dto;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional
    public void scheduledFetchAndSaveLivePriceData() {
        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        ZonedDateTime nowIst = ZonedDateTime.now(istZone);
        LocalTime currentTime = nowIst.toLocalTime();

        // Run only during market hours (9:15 AM to 3:30 PM IST)
       // if ( fo("Running scheduled job to fetch and save live price data.");
       log.info("Running scheduled job to fetch and save live price data.");     
       fetchAndSaveLivePriceData(nowIst.toLocalDate());
       // }
    }
}
