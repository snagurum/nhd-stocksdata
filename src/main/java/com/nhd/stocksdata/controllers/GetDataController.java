package com.nhd.stocksdata.controllers;

import com.nhd.stocksdata.config.StocksDataProperties;
import com.nhd.stocksdata.models.StockStats;
import com.nhd.stocksdata.services.StockStatsService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GetDataController {
    private final StockStatsService stockStatsService;
    private final StocksDataProperties properties;

    @GetMapping("/stockStats")
    public List<StockStats> getStockStats(
            @RequestParam LocalDate fromDate,
            @RequestParam(required = false) List<String> tickers) {
        int pageNumber = 0;
        Page<StockStats> page;
        List<StockStats> results = new ArrayList<>();
        do {
            page =
                    stockStatsService.getStockStatsFromDate(
                            fromDate,
                            tickers,
                            PageRequest.of(pageNumber++, properties.getApiStockStatsPageSize()));
            results.addAll(page.getContent());
        } while (page.hasNext());
        return results;
    }
}
