package com.stockanalyzer.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ScripMaster {

    @CsvBindByName(column = "Exch")
    private String exch;

    @CsvBindByName(column = "ExchType")
    private String exchType;

    @CsvBindByName(column = "ScripCode")
    private int scripCode;

    @CsvBindByName(column = "Name")
    private String name;

    @CsvBindByName(column = "Expiry")
    private LocalDate expiry;

    @CsvBindByName(column = "Symbol")
    private String symbol;

    @CsvBindByName(column = "ScripType")
    private String scripType;

    @CsvBindByName(column = "StrikeRate")
    private double strikeRate;
}
