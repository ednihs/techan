package com.stockanalyzer.service;

import com.opencsv.bean.CsvToBeanBuilder;
import com.stockanalyzer.dto.ScripMaster;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@Getter
public class ScripMasterService {

    private List<ScripMaster> scrips = Collections.emptyList();

    @PostConstruct
    public void loadScripMaster() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("scripmaster.csv")) {
            if (is == null) {
                log.error("scripmaster.csv not found in resources");
                return;
            }
            scrips = new CsvToBeanBuilder<ScripMaster>(new InputStreamReader(is))
                    .withType(ScripMaster.class)
                    .build()
                    .parse();
            log.info("Loaded {} scrips from scripmaster.csv", scrips.size());
        } catch (Exception e) {
            log.error("Failed to load or parse scripmaster.csv", e);
        }
    }

    public Optional<ScripMaster> getActiveCrudeOilFuture() {
        return scrips.stream()
                .filter(s -> "MFO".equalsIgnoreCase(s.getExch()) )
                .filter(s -> s.getName() != null && s.getName().toUpperCase().startsWith("CRUDEOIL"))
                .min(Comparator.comparing(ScripMaster::getExpiry));
    }

    public String getScripCode(String symbol) {
        return scrips.stream()
                .filter(s -> symbol.equalsIgnoreCase(s.getSymbol()))
                .findFirst()
                .map(s -> String.valueOf(s.getScripCode()))
                .orElse(null);
    }
}
