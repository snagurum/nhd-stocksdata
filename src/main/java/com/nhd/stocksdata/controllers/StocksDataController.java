package com.nhd.stocksdata.controllers;

import com.nhd.stocksdata.dto.DataPage;
import com.nhd.stocksdata.dto.DataQuery;
import com.nhd.stocksdata.models.MomentumStats;
import com.nhd.stocksdata.models.MomentumZStats;
import com.nhd.stocksdata.models.MomentumZStatsScore;
import com.nhd.stocksdata.models.StockPrice;
import com.nhd.stocksdata.models.StockStats;
import com.nhd.stocksdata.services.MomentumService;
import com.nhd.stocksdata.services.StockPriceService;
import com.nhd.stocksdata.services.StockService;
import com.nhd.stocksdata.services.StockStatsService;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class StocksDataController {
    private final StockPriceService stockPrice;
    private final StockStatsService stockStats;
    private final MomentumService momentum;
    private final StockService stock;

    @PostMapping("/stock-prices/batch")
    public void saveStockPrice(@RequestBody List<StockPrice> rows) {
        stockPrice.saveAll(rows);
    }

    @PostMapping("/stock-prices/date")
    public DataPage<StockPrice> dateStockPrice(@RequestBody DataQuery q) {
        return DataPage.from(
                stockPrice.getStockPricesFromDate(q.getDate(), q.getTickers(), page(q)));
    }

    @PostMapping("/stock-prices/ticker")
    public DataPage<StockPrice> tickerStockPrice(@RequestBody DataQuery q) {
        return DataPage.from(stockPrice.getByTicker(q.getTicker(), q.getDate(), page(q)));
    }

    @PostMapping("/stock-prices/tickers")
    public Map<String, List<StockPrice>> tickersStockPrice(@RequestBody DataQuery q) {
        return stockPrice.getStockPricesForTickers(q.getTickers(), q.getDate());
    }

    @PostMapping("/stock-stats/batch")
    public void saveStockStats(@RequestBody List<StockStats> rows) {
        stockStats.saveAll(rows);
    }

    @PostMapping("/stock-stats/date")
    public DataPage<StockStats> dateStockStats(@RequestBody DataQuery q) {
        return DataPage.from(
                stockStats.getStockStatsFromDate(q.getDate(), q.getTickers(), page(q)));
    }

    @PostMapping("/stock-stats/ticker")
    public DataPage<StockStats> tickerStockStats(@RequestBody DataQuery q) {
        return DataPage.from(stockStats.findByTicker(q.getTicker(), q.getDate(), page(q)));
    }

    @PostMapping("/stock-stats/tickers")
    public Map<String, List<StockStats>> tickersStockStats(@RequestBody DataQuery q) {
        return stockStats.getStockStatsForTickers(q.getTickers(), q.getDate());
    }

    @PostMapping("/momentum-stats/batch")
    public void saveMomentumStats(@RequestBody List<MomentumStats> rows) {
        momentum.saveMomentumStats(rows);
    }

    @PostMapping("/momentum-stats/date")
    public DataPage<MomentumStats> dateMomentumStats(@RequestBody DataQuery q) {
        return DataPage.from(
                momentum.getMomentumStatsForDate(q.getDate(), q.getTickers(), page(q)));
    }

    @PostMapping("/momentum-z-stats/batch")
    public void saveMomentumZStats(@RequestBody List<MomentumZStats> rows) {
        momentum.saveMomentumZStats(rows);
    }

    @PostMapping("/momentum-z-scores/batch")
    public void saveMomentumZStatsScore(@RequestBody List<MomentumZStatsScore> rows) {
        momentum.saveMomentumZStatsScores(rows);
    }

    @PostMapping("/momentum-z-stats/date")
    public OptionalResult zDate(@RequestBody DataQuery q) {
        return new OptionalResult(
                momentum.getMomentumZStatsForDate(q.getDate(), q.getTickerListName()));
    }

    @Value
    @Builder
    public static class OptionalResult {
        MomentumZStats value;
    }

    @PostMapping("/active-tickers")
    public List<String> activeTickers() {
        return stock.getAllActiveTickers();
    }

    private PageRequest page(DataQuery q) {
        int pageNumber = q.getPage() == null ? 0 : q.getPage();
        int pageSize = q.getSize() == null ? 2000 : q.getSize();
        if (pageNumber < 0 || pageSize < 1 || pageSize > 10000)
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid page or size (max 10000)");
        return PageRequest.of(pageNumber, pageSize);
    }
}
