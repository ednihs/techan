package com.stockanalyzer.service;

import com.stockanalyzer.dto.MarketFeedData;
import com.stockanalyzer.entity.IntradayData;
import com.stockanalyzer.repository.IntradayDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntradayDataService {

    private final IntradayDataRepository intradayDataRepository;
    private final FivePaisaService fivePaisaService;

    @Transactional
    public void fetchAndStoreIntradayData(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return;
        }
        List<MarketFeedData> feeds = fivePaisaService.getMarketFeed(symbols);
        if (feeds == null || feeds.isEmpty()) {
            log.debug("No market feed data returned for intraday collection");
            return;
        }
        LocalDate tradeDate = LocalDate.now();
        LocalTime tradeTime = LocalTime.now().withSecond(0).withNano(0);
        feeds.stream()
                .filter(Objects::nonNull)
                .forEach(feed -> upsertBar(feed, tradeDate, tradeTime));
    }

    private void upsertBar(MarketFeedData feed, LocalDate tradeDate, LocalTime tradeTime) {
        String symbol = feed.getSymbol();
        if (symbol == null) {
            return;
        }
        BigDecimal price = feed.getLastTradedPrice() == null ? BigDecimal.ZERO : feed.getLastTradedPrice();
        long volume = feed.getVolume();
        intradayDataRepository.findFirstBySymbolAndTradeDateAndTradeTime(symbol, tradeDate, tradeTime)
                .ifPresentOrElse(existing -> updateBar(existing, price, volume),
                        () -> createBar(symbol, tradeDate, tradeTime, price, volume));
    }

    private void updateBar(IntradayData existing, BigDecimal price, long volume) {
        existing.setClosePrice(price);
        BigDecimal currentHigh = existing.getHighPrice() == null ? price : existing.getHighPrice();
        BigDecimal currentLow = existing.getLowPrice() == null ? price : existing.getLowPrice();
        existing.setHighPrice(currentHigh.max(price));
        existing.setLowPrice(currentLow.min(price));
        existing.setVolume(volume);
        intradayDataRepository.save(existing);
    }

    private void createBar(String symbol, LocalDate tradeDate, LocalTime tradeTime, BigDecimal price, long volume) {
        IntradayData bar = new IntradayData();
        bar.setSymbol(symbol);
        bar.setTradeDate(tradeDate);
        bar.setTradeTime(tradeTime);
        bar.setOpenPrice(price);
        bar.setHighPrice(price);
        bar.setLowPrice(price);
        bar.setClosePrice(price);
        bar.setVolume(volume);
        intradayDataRepository.save(bar);
    }

    @Transactional(readOnly = true)
    public List<IntradayData> getBars(String symbol, LocalDate date, LocalTime from, LocalTime to) {
        if (symbol == null || date == null || from == null || to == null) {
            return Collections.emptyList();
        }
        return intradayDataRepository.findBySymbolAndTradeDateAndTradeTimeBetweenOrderByTradeTimeAsc(symbol, date, from, to);
    }
}
