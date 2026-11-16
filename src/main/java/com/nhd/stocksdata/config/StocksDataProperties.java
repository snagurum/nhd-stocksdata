package com.nhd.stocksdata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "stocksdata")
public class StocksDataProperties {
    private int paginationPageSize = 2000;
    private int apiStockStatsPageSize = 1000;
    private int persistenceJdbcBatchSize = 10000;
}
