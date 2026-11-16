package com.nhd.stocksdata.services;

import com.nhd.stocksdata.config.StocksDataProperties;
import com.nhd.stocksdata.models.StockStats;
import com.nhd.stocksdata.repository.StockStatsRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import org.hibernate.Session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class StockStatsService {

    private final StockStatsRepository stockStatsRepository;

    private final EntityManager entityManager;

    private final StocksDataProperties properties;

    public List<StockStats> getStockStatsFromDate(LocalDate date, List<String> tickers) {
        int pageNumber = 0, pageSize = properties.getPaginationPageSize();
        Page<StockStats> stockStatsPage;
        List<StockStats> stockStats = new ArrayList<>();
        do {
            PageRequest pageable = PageRequest.of(pageNumber, pageSize);
            stockStatsPage = getStockStatsFromDate(date, tickers, pageable);
            stockStats.addAll(stockStatsPage.getContent());
            pageNumber++;
        } while (stockStatsPage.hasNext());
        return stockStats;
    }

    public Page<StockStats> findByPriceDate(LocalDate priceDate, Pageable pageable) {
        return stockStatsRepository.findByPriceDate(priceDate, pageable);
    }

    public Page<StockStats> getStockStatsFromDate(
            LocalDate date, List<String> tickers, Pageable pageable) {
        Page<StockStats> stockStatsPage;
        if (null == tickers || tickers.size() == 0 || "***ALL***".equals(tickers.get(0)))
            stockStatsPage = stockStatsRepository.findByPriceDate(date, pageable);
        else if (tickers.size() > 1)
            stockStatsPage =
                    stockStatsRepository.findByPriceDateAndTickerIn(date, tickers, pageable);
        else
            stockStatsPage =
                    stockStatsRepository.findByPriceDateAndTickerStartingWith(
                            date, "" + tickers.get(0).charAt(0), pageable);
        return stockStatsPage;
    }

    public Page<StockStats> findByTicker(String ticker, LocalDate fromDate, Pageable pageable) {
        if (fromDate == null)
            return stockStatsRepository.findByTickerOrderByPriceDateAsc(ticker, pageable);
        else
            return stockStatsRepository.findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
                    ticker, fromDate, pageable);
    }

    public Map<String, List<StockStats>> getStockStatsForTickers(
            List<String> tickers, LocalDate startDate) {
        return stockStatsRepository
                .findByTickerInAndPriceDateGreaterThanEqualOrderByPriceDateAsc(tickers, startDate)
                .stream()
                .collect(Collectors.groupingBy(StockStats::getTicker));
    }

    public Page<StockStats> findByPriceDateAndTickerStartingWith(
            LocalDate date, String ticker, Pageable pageable) {
        return stockStatsRepository.findByPriceDateAndTickerStartingWith(date, ticker, pageable);
    }

    public Page<StockStats> findByPriceDateAndTickerIn(
            LocalDate date, List<String> tickers, Pageable pageable) {
        return stockStatsRepository.findByPriceDateAndTickerIn(date, tickers, pageable);
    }

    @Transactional
    public void saveAll(List<StockStats> stockStats) {
        if (stockStats == null || stockStats.isEmpty()) {
            return;
        }

        long start = System.nanoTime();
        entityManager
                .createNativeQuery(
                        """
                            CREATE TEMP TABLE temp_stock_stats (
                                ticker VARCHAR(50),
                                price_date DATE,
                                daily_return NUMERIC(18,8),
                                ln_daily_return NUMERIC(18,8),
                                dma50      NUMERIC(18,4),
                                dma200     NUMERIC(18,4),
                                return_1yr numeric(18,8),
                                return_6m  numeric(18,8),
                                return_3m  numeric(18,8),
                                return_1m  numeric(18,8),
                                std_dev_1yr numeric(18,8),
                                std_dev_6m  numeric(18,8),
                                std_dev_3m  numeric(18,8),
                                std_dev_1m  numeric(18,8),
                                rsi14 numeric(18,8),
                                rsi7 numeric(18,8),
                                return_2_volatility numeric(18,8),
                                drop_percentage numeric(18,8)
                            ) ON COMMIT DROP;
                        """)
                .executeUpdate();

        long tempCreated = System.nanoTime();
        Session session = entityManager.unwrap(Session.class);
        session.doWork(
                connection -> {
                    String sql =
                            """
                            INSERT INTO temp_stock_stats (
                                ticker, price_date, daily_return, ln_daily_return, dma50, dma200,
                                return_1yr, return_6m, return_3m, return_1m,
                                std_dev_1yr, std_dev_6m, std_dev_3m, std_dev_1m,
                                rsi14, rsi7, return_2_volatility, drop_percentage
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """;
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        int batchSize = properties.getPersistenceJdbcBatchSize();
                        int count = 0;

                        for (StockStats ss : stockStats) {
                            ps.setString(1, ss.getTicker());
                            ps.setDate(2, java.sql.Date.valueOf(ss.getPriceDate()));
                            ps.setBigDecimal(3, ss.getDailyReturn());
                            ps.setBigDecimal(4, ss.getLnDailyReturn());
                            ps.setBigDecimal(5, ss.getDma50());
                            ps.setBigDecimal(6, ss.getDma200());
                            ps.setBigDecimal(7, ss.getReturn1yr());
                            ps.setBigDecimal(8, ss.getReturn6m());
                            ps.setBigDecimal(9, ss.getReturn3m());
                            ps.setBigDecimal(10, ss.getReturn1m());
                            ps.setBigDecimal(11, ss.getStdDev1yr());
                            ps.setBigDecimal(12, ss.getStdDev6m());
                            ps.setBigDecimal(13, ss.getStdDev3m());
                            ps.setBigDecimal(14, ss.getStdDev1m());
                            ps.setBigDecimal(15, ss.getRsi14());
                            ps.setBigDecimal(16, ss.getRsi7());
                            ps.setBigDecimal(17, ss.getReturn2Volatility());
                            ps.setBigDecimal(18, ss.getDropPercentage());
                            ps.addBatch();
                            if (++count % batchSize == 0) {
                                ps.executeBatch();
                            }
                        }
                        ps.executeBatch();
                    }
                });

        long batchInserted = System.nanoTime();

        entityManager.createNativeQuery("ANALYZE temp_stock_stats").executeUpdate();
        long analyze = System.nanoTime();

        int affected =
                entityManager
                        .createNativeQuery(
                                """
    INSERT INTO nse.stock_stats (
        ticker, price_date,
        daily_return, ln_daily_return,
        dma50, dma200,
        return_1yr, return_6m, return_3m, return_1m,
        std_dev_1yr, std_dev_6m, std_dev_3m, std_dev_1m, rsi14, rsi7,
        return_2_volatility, drop_percentage
    )
    SELECT
        ticker, price_date,
        daily_return, ln_daily_return,
        dma50, dma200,
        return_1yr, return_6m, return_3m, return_1m,
        std_dev_1yr, std_dev_6m, std_dev_3m, std_dev_1m, rsi14, rsi7,
        return_2_volatility, drop_percentage
    FROM temp_stock_stats
    ON CONFLICT (ticker, price_date)
    DO UPDATE SET
        daily_return = COALESCE(EXCLUDED.daily_return, stock_stats.daily_return),
        ln_daily_return = COALESCE(EXCLUDED.ln_daily_return, stock_stats.ln_daily_return),
        dma50 = COALESCE(EXCLUDED.dma50, stock_stats.dma50),
        dma200 = COALESCE(EXCLUDED.dma200, stock_stats.dma200),
        return_1yr = COALESCE(EXCLUDED.return_1yr, stock_stats.return_1yr),
        return_6m = COALESCE(EXCLUDED.return_6m, stock_stats.return_6m),
        return_3m = COALESCE(EXCLUDED.return_3m, stock_stats.return_3m),
        return_1m = COALESCE(EXCLUDED.return_1m, stock_stats.return_1m),
        std_dev_1yr = COALESCE(EXCLUDED.std_dev_1yr, stock_stats.std_dev_1yr),
        std_dev_6m = COALESCE(EXCLUDED.std_dev_6m, stock_stats.std_dev_6m),
        std_dev_3m = COALESCE(EXCLUDED.std_dev_3m, stock_stats.std_dev_3m),
        std_dev_1m = COALESCE(EXCLUDED.std_dev_1m, stock_stats.std_dev_1m),
        rsi14 = COALESCE(EXCLUDED.rsi14, stock_stats.rsi14),
        rsi7 = COALESCE(EXCLUDED.rsi7, stock_stats.rsi7),
        return_2_volatility = COALESCE(EXCLUDED.return_2_volatility, stock_stats.return_2_volatility),
        drop_percentage = COALESCE(EXCLUDED.drop_percentage, stock_stats.drop_percentage)
    WHERE
        ROW(
            stock_stats.daily_return,
            stock_stats.ln_daily_return,
            stock_stats.dma50,
            stock_stats.dma200,
            stock_stats.return_1yr,
            stock_stats.return_6m,
            stock_stats.return_3m,
            stock_stats.return_1m,
            stock_stats.std_dev_1yr,
            stock_stats.std_dev_6m,
            stock_stats.std_dev_3m,
            stock_stats.std_dev_1m,
            stock_stats.rsi14,
            stock_stats.rsi7,
            stock_stats.return_2_volatility,
            stock_stats.drop_percentage
        )
        IS DISTINCT FROM
        ROW(
            COALESCE(EXCLUDED.daily_return, stock_stats.daily_return),
            COALESCE(EXCLUDED.ln_daily_return, stock_stats.ln_daily_return),
            COALESCE(EXCLUDED.dma50, stock_stats.dma50),
            COALESCE(EXCLUDED.dma200, stock_stats.dma200),
            COALESCE(EXCLUDED.return_1yr, stock_stats.return_1yr),
            COALESCE(EXCLUDED.return_6m, stock_stats.return_6m),
            COALESCE(EXCLUDED.return_3m, stock_stats.return_3m),
            COALESCE(EXCLUDED.return_1m, stock_stats.return_1m),
            COALESCE(EXCLUDED.std_dev_1yr, stock_stats.std_dev_1yr),
            COALESCE(EXCLUDED.std_dev_6m, stock_stats.std_dev_6m),
            COALESCE(EXCLUDED.std_dev_3m, stock_stats.std_dev_3m),
            COALESCE(EXCLUDED.std_dev_1m, stock_stats.std_dev_1m),
            COALESCE(EXCLUDED.rsi14, stock_stats.rsi14),
            COALESCE(EXCLUDED.rsi7, stock_stats.rsi7),
            COALESCE(EXCLUDED.return_2_volatility, stock_stats.return_2_volatility),
            COALESCE(EXCLUDED.drop_percentage, stock_stats.drop_percentage)
        );
""")
                        .executeUpdate();

        long upsertCompleted = System.nanoTime();
        log.info(
                "StockStats persistence: records={}, tempTable={}ms, "
                        + "jdbcBatch={}ms, upsert={}ms, affected={}, total={}ms",
                stockStats.size(),
                (tempCreated - start) / 1_000_000,
                (batchInserted - tempCreated) / 1_000_000,
                (analyze - batchInserted) / 1_000_000,
                (upsertCompleted - analyze) / 1_000_000,
                affected,
                (upsertCompleted - start) / 1_000_000);
    }
}
