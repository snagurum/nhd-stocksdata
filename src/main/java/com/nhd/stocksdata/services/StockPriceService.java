package com.nhd.stocksdata.services;

import com.nhd.stocksdata.config.StocksDataProperties;
import com.nhd.stocksdata.models.StockPrice;
import com.nhd.stocksdata.repository.StockPriceRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import org.hibernate.Session;

import lombok.RequiredArgsConstructor;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class StockPriceService {

    private final StockPriceRepository stockPriceRepository;

    private final EntityManager entityManager;

    private final StocksDataProperties properties;

    public List<StockPrice> getStockPricesFromDate(LocalDate date, List<String> tickers) {
        int pageNumber = 0, pageSize = properties.getPaginationPageSize();
        Page<StockPrice> stockPricePage;
        List<StockPrice> stockPrices = new ArrayList<>();
        do {
            PageRequest pageable = PageRequest.of(pageNumber, pageSize);
            stockPricePage = getStockPricesFromDate(date, tickers, pageable);
            stockPrices.addAll(stockPricePage.getContent());
            pageNumber++;
        } while (stockPricePage.hasNext());
        return stockPrices;
    }

    public Page<StockPrice> findByPriceDate(LocalDate priceDate, Pageable pageable) {
        return stockPriceRepository.findByPriceDate(priceDate, pageable);
    }

    public Page<StockPrice> getStockPricesFromDate(
            LocalDate date, List<String> tickers, Pageable pageable) {
        Page<StockPrice> stockPricePage;
        if (null == tickers || tickers.size() == 0)
            stockPricePage = stockPriceRepository.findByPriceDate(date, pageable);
        else if (tickers.size() > 1)
            stockPricePage =
                    stockPriceRepository.findByPriceDateAndTickerIn(date, tickers, pageable);
        else
            stockPricePage =
                    stockPriceRepository.findByPriceDateAndTickerStartingWith(
                            date, "" + tickers.get(0).charAt(0), pageable);
        return stockPricePage;
    }

    public Page<StockPrice> getByTicker(String ticker, LocalDate fromDate, Pageable pageable) {
        if (fromDate == null)
            return stockPriceRepository.findByTickerOrderByPriceDateAsc(ticker, pageable);
        else
            return stockPriceRepository.findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
                    ticker, fromDate, pageable);
    }

    public Map<String, List<StockPrice>> getStockPricesForTickers(
            List<String> tickers, LocalDate startDate) {
        if (startDate == null)
            return stockPriceRepository.findByTickerInOrderByPriceDateAsc(tickers).stream()
                    .collect(Collectors.groupingBy(StockPrice::getTicker));
        else
            return stockPriceRepository
                    .findByTickerInAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
                            tickers, startDate)
                    .stream()
                    .collect(Collectors.groupingBy(StockPrice::getTicker));
    }

    public Page<StockPrice> findByPriceDateAndTickerStartingWith(
            LocalDate date, String ticker, Pageable pageable) {
        return stockPriceRepository.findByPriceDateAndTickerStartingWith(date, ticker, pageable);
    }

    public Page<StockPrice> findByPriceDateAndTickerIn(
            LocalDate date, List<String> tickers, Pageable pageable) {
        return stockPriceRepository.findByPriceDateAndTickerIn(date, tickers, pageable);
    }

    public Page<StockPrice> findByTickerAndPriceDateGreaterThanEqual(
            String ticker, LocalDate date, Pageable pageable) {
        return stockPriceRepository.findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
                ticker, date, pageable);
    }

    @Transactional
    public void saveAll(List<StockPrice> stockPrice) {
        if (stockPrice == null || stockPrice.isEmpty()) {
            return;
        }

        entityManager
                .createNativeQuery(
                        """
                            CREATE TEMP TABLE temp_stock_price (
                                ticker           VARCHAR(50),
                                price_date       DATE,
                                series           VARCHAR(10),
                                open             NUMERIC(18, 4),
                                high             NUMERIC(18, 4),
                                low              NUMERIC(18, 4),
                                prev_close       NUMERIC(18, 4),
                                close            NUMERIC(18, 4),
                                vwap             NUMERIC(18, 4),
                                high52           NUMERIC(18, 4),
                                low52            NUMERIC(18, 4),
                                volume           BIGINT,
                                value            NUMERIC(20, 2),
                                no_of_trades     BIGINT,
                                ltp              NUMERIC(18, 4),
                                ffmc             BIGINT
                            ) ON COMMIT DROP;
                        """)
                .executeUpdate();

        Session session = entityManager.unwrap(Session.class);
        session.doWork(
                connection -> {
                    String sql =
                            "INSERT INTO temp_stock_price (ticker, price_date, series, open, high,"
                                + " low, prev_close, close, vwap, high52, low52, volume, value,"
                                + " no_of_trades, ltp, ffmc) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
                                + " ?, ?, ?, ?, ?, ? )";
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        int batchSize = properties.getPersistenceJdbcBatchSize();
                        int count = 0;

                        for (StockPrice ss : stockPrice) {
                            ps.setString(1, ss.getTicker());
                            ps.setDate(2, java.sql.Date.valueOf(ss.getPriceDate()));
                            ps.setString(3, ss.getSeries());
                            ps.setBigDecimal(4, ss.getOpen());
                            ps.setBigDecimal(5, ss.getHigh());
                            ps.setBigDecimal(6, ss.getLow());
                            ps.setBigDecimal(7, ss.getPrevClose());
                            ps.setBigDecimal(8, ss.getClose());
                            ps.setBigDecimal(9, ss.getVwap());
                            ps.setBigDecimal(10, ss.getHigh52());
                            ps.setBigDecimal(11, ss.getLow52());
                            ps.setObject(12, ss.getVolume());
                            ps.setBigDecimal(13, ss.getValue());
                            ps.setObject(14, ss.getNoOfTrades());
                            ps.setBigDecimal(15, ss.getLtp());
                            ps.setObject(16, ss.getFfmc());
                            ps.addBatch();
                            if (++count % batchSize == 0) {
                                ps.executeBatch();
                            }
                        }
                        ps.executeBatch();
                    }
                });
        entityManager.flush();

        entityManager
                .createNativeQuery(
                        """
INSERT INTO nse.stock_price (ticker, price_date, series, open, high, low, prev_close, close, vwap, high52, low52, volume, value, no_of_trades, ltp, ffmc)
SELECT ticker, price_date, series, open, high, low, prev_close, close, vwap, high52, low52, volume, value, no_of_trades, ltp, ffmc
FROM temp_stock_price
ON CONFLICT (ticker, price_date)
DO UPDATE SET
  series        = COALESCE(EXCLUDED.series       , stock_price.series         ),
  open          = COALESCE(EXCLUDED.open         , stock_price.open           ),
  high          = COALESCE(EXCLUDED.high         , stock_price.high           ),
  low           = COALESCE(EXCLUDED.low          , stock_price.low            ),
  prev_close    = COALESCE(EXCLUDED.prev_close   , stock_price.prev_close     ),
  close         = COALESCE(EXCLUDED.close        , stock_price.close          ),
  vwap          = COALESCE(EXCLUDED.vwap         , stock_price.vwap           ),
  high52        = COALESCE(EXCLUDED.high52       , stock_price.high52         ),
  low52         = COALESCE(EXCLUDED.low52        , stock_price.low52          ),
  volume        = COALESCE(EXCLUDED.volume       , stock_price.volume         ),
  value         = COALESCE(EXCLUDED.value        , stock_price.value          ),
  no_of_trades  = COALESCE(EXCLUDED.no_of_trades , stock_price.no_of_trades   ),
  ltp           = COALESCE(EXCLUDED.ltp          , stock_price.ltp            ),
  ffmc          = COALESCE(EXCLUDED.ffmc         , stock_price.ffmc           );
""")
                .executeUpdate();

        entityManager.clear();
    }
}
