package com.nhd.stocksdata.controllers;

import com.nhd.stocksdata.models.StockPrice;
import com.nhd.stocksdata.models.StockStats;
import com.nhd.stocksdata.services.MomentumService;
import com.nhd.stocksdata.services.StockPriceService;
import com.nhd.stocksdata.services.StockService;
import com.nhd.stocksdata.services.StockStatsService;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StocksDataControllerTest {
    private MockMvc mvc;
    private StockPriceService prices;
    private StockStatsService stats;
    private MomentumService momentum;
    private StockService stocks;

    @BeforeEach
    void setup() {
        prices = mock(StockPriceService.class);
        stats = mock(StockStatsService.class);
        momentum = mock(MomentumService.class);
        stocks = mock(StockService.class);
        mvc =
                MockMvcBuilders.standaloneSetup(
                                new StocksDataController(prices, stats, momentum, stocks))
                        .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void stockStatsIndicatorsSurviveHttpWriteAndRead() throws Exception {
        mvc.perform(
                        post("/internal/stock-stats/batch")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
[{"ticker":"TCS","priceDate":"2026-01-01","rsi14":63.12345678,"rsi7":71.87654321,"return2Volatility":1.23456789,"dropPercentage":-12.34567890}]
"""))
                .andExpect(status().isOk());
        ArgumentCaptor<List<StockStats>> captor = ArgumentCaptor.forClass(List.class);
        verify(stats).saveAll(captor.capture());
        StockStats row = captor.getValue().get(0);
        assertEquals(0, new BigDecimal("63.12345678").compareTo(row.getRsi14()));
        assertEquals(0, new BigDecimal("71.87654321").compareTo(row.getRsi7()));
        assertEquals(0, new BigDecimal("1.23456789").compareTo(row.getReturn2Volatility()));
        assertEquals(0, new BigDecimal("-12.34567890").compareTo(row.getDropPercentage()));
        when(stats.getStockStatsFromDate(LocalDate.of(2026, 1, 1), null, PageRequest.of(0, 2000)))
                .thenReturn(new PageImpl<>(List.of(row)));
        mvc.perform(
                        post("/internal/stock-stats/date")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"date":"2026-01-01"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rsi14").value(63.12345678))
                .andExpect(jsonPath("$.content[0].rsi7").value(71.87654321))
                .andExpect(jsonPath("$.content[0].return2Volatility").value(1.23456789))
                .andExpect(jsonPath("$.content[0].dropPercentage").value(-12.34567890));
    }

    @Test
    void dateQueryReturnsStablePageAndPreservesFilters() throws Exception {
        var date = LocalDate.of(2026, 1, 1);
        var row = new StockPrice();
        row.setTicker("TCS");
        row.setPriceDate(date);
        row.setClose(new BigDecimal("123.4500"));
        when(prices.getStockPricesFromDate(date, List.of("TCS", "INFY"), PageRequest.of(1, 1)))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(1, 1), 3));
        mvc.perform(
                        post("/internal/stock-prices/date")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
{"date":"2026-01-01","tickers":["TCS","INFY"],"page":1,"size":1}
"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].ticker").value("TCS"))
                .andExpect(jsonPath("$.content[0].priceDate").value("2026-01-01"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void batchDeserializesIntoEntityAndDelegatesToTransactionalService() throws Exception {
        mvc.perform(
                        post("/internal/stock-prices/batch")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
[{"ticker":"TCS","priceDate":"2026-01-01","close":123.4500}]
"""))
                .andExpect(status().isOk());
        ArgumentCaptor<List<StockPrice>> captor = ArgumentCaptor.forClass(List.class);
        verify(prices).saveAll(captor.capture());
        assertEquals(LocalDate.of(2026, 1, 1), captor.getValue().get(0).getPriceDate());
        assertEquals(0, new BigDecimal("123.4500").compareTo(captor.getValue().get(0).getClose()));
    }

    @Test
    void allDestinationsHaveWriteRoutes() throws Exception {
        for (String resource :
                List.of("stock-stats", "momentum-stats", "momentum-z-stats", "momentum-z-scores")) {
            mvc.perform(
                            post("/internal/" + resource + "/batch")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("[]"))
                    .andExpect(status().isOk());
        }
        verify(stats).saveAll(List.of());
        verify(momentum).saveMomentumStats(List.of());
        verify(momentum).saveMomentumZStats(List.of());
        verify(momentum).saveMomentumZStatsScores(List.of());
    }

    @Test
    void missingZStatsAndActiveTickersHaveExplicitContracts() throws Exception {
        mvc.perform(
                        post("/internal/momentum-z-stats/date")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"date":"2026-01-01","tickerListName":"Nifty50"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"value\":null}"));
        when(stocks.getAllActiveTickers()).thenReturn(List.of("TCS"));
        mvc.perform(
                        post("/internal/active-tickers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"TCS\"]"));
    }

    @Test
    void rejectsInvalidPageSizeBeforeCallingDatabase() throws Exception {
        mvc.perform(
                        post("/internal/stock-prices/date")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"date":"2026-01-01","page":0,"size":10001}
                                        """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(prices);
    }
}
