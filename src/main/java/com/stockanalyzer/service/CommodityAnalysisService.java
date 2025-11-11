package com.stockanalyzer.service;

import com.stockanalyzer.dto.ScripMaster;
import com.stockanalyzer.dto.TechnicalIndicatorDTO;
import com.stockanalyzer.entity.PriceData;
import com.stockanalyzer.util.MarketTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommodityAnalysisService {

    private final FivePaisaService fivePaisaService;
    private final TechnicalAnalysisService technicalAnalysisService;
    private final ScripMasterService scripMasterService;

    public List<TechnicalIndicatorDTO> getCrudeOilIndicators(String interval, LocalDate date) {
        /*Optional<ScripMaster> activeCrudeOilFuture = scripMasterService.getActiveCrudeOilFuture();
        if (activeCrudeOilFuture.isEmpty()) {
            return Collections.emptyList();
        }
        int scripCode = activeCrudeOilFuture.get().getScripCode();*/
        int scripCode = 457886;

        LocalDate startDate = date.minusDays(60); // Fetch enough data for indicator calculation
        List<PriceData> priceDataList = fivePaisaService.getHistoricalData(scripCode, interval, startDate, date);

        if (priceDataList == null || priceDataList.isEmpty()) {
            return Collections.emptyList();
        }

        return priceDataList.stream()
                .filter(priceData -> MarketTimeUtil.isMarketOpen(priceData.getCreatedAt().toLocalTime()))
                .map(priceData -> {
                    List<PriceData> historicalDataForIndicator = priceDataList.stream()
                            .filter(p -> !p.getCreatedAt().isAfter(priceData.getCreatedAt()))
                            .collect(Collectors.toList());
                    return technicalAnalysisService.calculateTechnicalIndicators(priceData, historicalDataForIndicator);
                })
                .filter(Objects::nonNull)
                .map(technicalAnalysisService::mapToDTO)
                .collect(Collectors.toList());
    }
}
