package com.stockanalyzer.repository;

import com.stockanalyzer.entity.IntradayData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface IntradayDataRepository extends JpaRepository<IntradayData, Long> {

    List<IntradayData> findBySymbolAndTradeDateAndTradeTimeBetweenOrderByTradeTimeAsc(String symbol,
                                                                                      LocalDate tradeDate,
                                                                                      LocalTime startTime,
                                                                                      LocalTime endTime);

    Optional<IntradayData> findFirstBySymbolAndTradeDateAndTradeTime(String symbol,
                                                                     LocalDate tradeDate,
                                                                     LocalTime tradeTime);
}
