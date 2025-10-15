package com.stockanalyzer.service;

import com.stockanalyzer.repository.PriceDataRepository;
import com.stockanalyzer.repository.TechnicalIndicatorRepository;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class TechnicalAnalysisService {

    private final PriceDataRepository priceDataRepository;
    private final TechnicalIndicatorRepository technicalIndicatorRepository;

    public TechnicalAnalysisService(PriceDataRepository priceDataRepository, TechnicalIndicatorRepository technicalIndicatorRepository) {
        this.priceDataRepository = priceDataRepository;
        this.technicalIndicatorRepository = technicalIndicatorRepository;
    }

    // Methods for calculating technical indicators will be implemented here
}
