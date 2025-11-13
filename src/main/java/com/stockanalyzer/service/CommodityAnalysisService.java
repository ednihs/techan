package com.stockanalyzer.service;

import com.stockanalyzer.dto.ScripMaster;
import com.stockanalyzer.dto.TechnicalIndicatorDTO;
import com.stockanalyzer.entity.PriceData;
import com.stockanalyzer.util.MarketTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommodityAnalysisService {

    private final FivePaisaService fivePaisaService;
    private final TechnicalAnalysisService technicalAnalysisService;
    private final ScripMasterService scripMasterService;

    public List<TechnicalIndicatorDTO> getCrudeOilIndicators(String interval, LocalDate date) {
        /*Optional<ScripMaster> activeCrudeOilFuture = scripMasterService.getActiveCrudeOilFuture();
        if (activeCrudeOilFuture.isEmpty()) {
            log.warn("No active crude oil future contract found in scrip master.");
            return Collections.emptyList();
        }*/
        //int scripCode = activeCrudeOilFuture.get().getScripCode();
        int scripCode =457886;
        //String symbol = activeCrudeOilFuture.get().getName();
        String symbol = "CRUDEOIL";

        LocalDate startDate = date.minusDays(60); // Fetch enough data for indicator calculation
        List<PriceData> priceDataList = fivePaisaService.getHistoricalData(scripCode, interval, startDate, date);

        if (priceDataList == null || priceDataList.isEmpty()) {
            return Collections.emptyList();
        }

        // Set the correct symbol for all price data records
        priceDataList.forEach(pd -> pd.setSymbol(symbol));

        return priceDataList.stream()
                .filter(priceData -> MarketTimeUtil.isMarketOpen(priceData.getCreatedAt().toLocalTime()))
                .map(priceData -> {
                    List<PriceData> historicalDataForIndicator = priceDataList.stream()
                            .filter(p -> !p.getCreatedAt().isAfter(priceData.getCreatedAt()))
                            .collect(Collectors.toList());
                    return technicalAnalysisService.calculateIndicatorsFromHistory(historicalDataForIndicator, priceData.getTradeDate());
                })
                .filter(Objects::nonNull)
                .map(technicalAnalysisService::mapToDTO)
                .collect(Collectors.toList());
    }
}
