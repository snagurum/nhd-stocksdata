package com.nhd.stocksdata.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQuery {
    private LocalDate date;
    private List<String> tickers;
    private String ticker;
    private String tickerListName;

    @Builder.Default
    @JsonSetter(nulls = Nulls.SKIP)
    private Integer page = 0;

    @Builder.Default
    @JsonSetter(nulls = Nulls.SKIP)
    private Integer size = 2000;
}
