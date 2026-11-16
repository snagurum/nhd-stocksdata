package com.nhd.stocksdata.services;

import com.nhd.stocksdata.config.StocksDataProperties;
import com.nhd.stocksdata.models.MomentumStats;
import com.nhd.stocksdata.models.MomentumZStats;
import com.nhd.stocksdata.models.MomentumZStatsScore;
import com.nhd.stocksdata.repository.MomentumStatsRepository;
import com.nhd.stocksdata.repository.MomentumZStatsRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import org.hibernate.Session;

import lombok.RequiredArgsConstructor;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
public class MomentumService {
    private final MomentumStatsRepository momentumStatsRepository;
    private final MomentumZStatsRepository momentumZStatsRepository;
    private final EntityManager entityManager;
    private final StocksDataProperties properties;

    public Page<MomentumStats> getByTicker(String ticker, LocalDate fromDate, Pageable pageable) {
        if (fromDate == null)
            return momentumStatsRepository.findByTickerOrderByPriceDate(ticker, pageable);
        else
            return momentumStatsRepository
                    .findByTickerAndPriceDateGreaterThanEqualOrderByPriceDateAsc(
                            ticker, fromDate, pageable);
    }

    public Page<MomentumStats> findByPriceDate(LocalDate priceDate, Pageable pageable) {
        return momentumStatsRepository.findByPriceDate(priceDate, pageable);
    }

    public Page<MomentumStats> getMomentumStatsForDate(
            LocalDate date, List<String> tickers, Pageable pageable) {
        Page<MomentumStats> momentumStatsPage;
        if (null == tickers || tickers.size() == 0 || "***ALL***".equals(tickers.get(0)))
            momentumStatsPage = momentumStatsRepository.findByPriceDate(date, pageable);
        else if (tickers.size() > 1)
            momentumStatsPage =
                    momentumStatsRepository.findByPriceDateAndTickerIn(date, tickers, pageable);
        else
            momentumStatsPage =
                    momentumStatsRepository.findByPriceDateAndTickerStartingWith(
                            date, "" + tickers.get(0).charAt(0), pageable);
        return momentumStatsPage;
    }

    public Page<MomentumStats> findByPriceDateAndTickerStartingWith(
            LocalDate date, String ticker, Pageable pageable) {
        return momentumStatsRepository.findByPriceDateAndTickerStartingWith(date, ticker, pageable);
    }

    public Page<MomentumStats> findByPriceDateAndTickerIn(
            LocalDate date, List<String> tickers, Pageable pageable) {
        return momentumStatsRepository.findByPriceDateAndTickerIn(date, tickers, pageable);
    }

    @Transactional
    public void saveMomentumStats(List<MomentumStats> momentumStats) {
        if (momentumStats == null || momentumStats.isEmpty()) {
            return;
        }

        entityManager
                .createNativeQuery(
                        """
                            CREATE TEMP TABLE temp_momentum_stats (
                                ticker                      VARCHAR(20) NOT NULL,
                                price_date                  DATE NOT NULL,
                                daily_return                NUMERIC(18, 8),
                                ln_daily_return             NUMERIC(18, 8),
                                mean_1yr_ln_daily_return    NUMERIC(18, 8),
                                sum_1yr_ln_daily_return     NUMERIC(18, 8),
                                n_1yr                       NUMERIC(10),
                                std_dev_1yr_ln_daily_return NUMERIC(18, 8),
                                std_Dev_1yr_Annualized      NUMERIC(18, 8),
                                price_return_1yr            NUMERIC(18, 8),
                                mr12                        NUMERIC(18, 8),
                                mean_6m_ln_daily_return     NUMERIC(18, 8),
                                sum_6m_ln_daily_return      NUMERIC(18, 8),
                                n_6m                        NUMERIC(18, 8),
                                std_dev_6m_ln_daily_return  NUMERIC(18, 8),
                                std_dev_6m_annualized       NUMERIC(18, 8),
                                price_return_6m             NUMERIC(18, 8),
                                mr6                         NUMERIC(18, 8)
                            ) ON COMMIT DROP;
                        """)
                .executeUpdate();

        Session session = entityManager.unwrap(Session.class);
        session.doWork(
                connection -> {
                    String sql =
                            "INSERT INTO temp_momentum_stats ( ticker, price_date, daily_return,"
                                + " ln_daily_return, mean_1yr_ln_daily_return,"
                                + " sum_1yr_ln_daily_return, n_1yr, std_dev_1yr_ln_daily_return,"
                                + " std_Dev_1yr_Annualized, price_return_1yr, mr12,"
                                + " mean_6m_ln_daily_return, sum_6m_ln_daily_return, n_6m,"
                                + " std_dev_6m_ln_daily_return, std_dev_6m_annualized,"
                                + " price_return_6m, mr6) values( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
                                + " ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        int batchSize = properties.getPersistenceJdbcBatchSize();
                        int count = 0;

                        for (MomentumStats ms : momentumStats) {
                            ps.setString(1, ms.getTicker());
                            ps.setDate(2, java.sql.Date.valueOf(ms.getPriceDate()));
                            ps.setBigDecimal(3, ms.getDailyReturn());
                            ps.setBigDecimal(4, ms.getLnDailyReturn());
                            ps.setBigDecimal(5, ms.getMean1yrLnDailyReturn());
                            ps.setBigDecimal(6, ms.getSum1yrLnDailyReturn());
                            ps.setObject(7, ms.getN1yr());
                            ps.setBigDecimal(8, ms.getStdDev1yrLnDailyReturn());
                            ps.setBigDecimal(9, ms.getStdDev1yrAnnualized());
                            ps.setBigDecimal(10, ms.getPriceReturn1yr());
                            ps.setBigDecimal(11, ms.getMr12());
                            ps.setBigDecimal(12, ms.getMean6mLnDailyReturn());
                            ps.setBigDecimal(13, ms.getSum6mLnDailyReturn());
                            ps.setObject(14, ms.getN6m());
                            ps.setBigDecimal(15, ms.getStdDev6mLnDailyReturn());
                            ps.setBigDecimal(16, ms.getStdDev6mAnnualized());
                            ps.setBigDecimal(17, ms.getPriceReturn6m());
                            ps.setBigDecimal(18, ms.getMr6());
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
INSERT INTO nse.momentum_stats ( ticker, price_date, daily_return, ln_daily_return, mean_1yr_ln_daily_return, sum_1yr_ln_daily_return, n_1yr, std_dev_1yr_ln_daily_return, std_Dev_1yr_Annualized, price_return_1yr, mr12, mean_6m_ln_daily_return, sum_6m_ln_daily_return, n_6m, std_dev_6m_ln_daily_return, std_dev_6m_annualized, price_return_6m, mr6)
    select ticker, price_date, daily_return, ln_daily_return, mean_1yr_ln_daily_return, sum_1yr_ln_daily_return, n_1yr, std_dev_1yr_ln_daily_return, std_Dev_1yr_Annualized, price_return_1yr, mr12, mean_6m_ln_daily_return, sum_6m_ln_daily_return, n_6m, std_dev_6m_ln_daily_return, std_dev_6m_annualized, price_return_6m, mr6
    from temp_momentum_stats
ON CONFLICT (ticker, price_date)
DO UPDATE SET
    daily_return                = COALESCE( EXCLUDED.daily_return                  , momentum_stats.daily_return                  ),
    ln_daily_return             = COALESCE( EXCLUDED.ln_daily_return               , momentum_stats.ln_daily_return               ),
    mean_1yr_ln_daily_return    = COALESCE( EXCLUDED.mean_1yr_ln_daily_return      , momentum_stats.mean_1yr_ln_daily_return      ),
    sum_1yr_ln_daily_return     = COALESCE( EXCLUDED.sum_1yr_ln_daily_return       , momentum_stats.sum_1yr_ln_daily_return       ),
    n_1yr                       = COALESCE( EXCLUDED.n_1yr                         , momentum_stats.n_1yr                         ),
    std_dev_1yr_ln_daily_return = COALESCE( EXCLUDED.std_dev_1yr_ln_daily_return   , momentum_stats.std_dev_1yr_ln_daily_return   ),
    std_Dev_1yr_Annualized      = COALESCE( EXCLUDED.std_Dev_1yr_Annualized        , momentum_stats.std_Dev_1yr_Annualized        ),
    price_return_1yr            = COALESCE( EXCLUDED.price_return_1yr              , momentum_stats.price_return_1yr              ),
    mr12                        = COALESCE( EXCLUDED.mr12                          , momentum_stats.mr12                          ),
    mean_6m_ln_daily_return     = COALESCE( EXCLUDED.mean_6m_ln_daily_return       , momentum_stats.mean_6m_ln_daily_return       ),
    sum_6m_ln_daily_return      = COALESCE( EXCLUDED.sum_6m_ln_daily_return        , momentum_stats.sum_6m_ln_daily_return        ),
    n_6m                        = COALESCE( EXCLUDED.n_6m                          , momentum_stats.n_6m                          ),
    std_dev_6m_ln_daily_return  = COALESCE( EXCLUDED.std_dev_6m_ln_daily_return    , momentum_stats.std_dev_6m_ln_daily_return    ),
    std_dev_6m_annualized       = COALESCE( EXCLUDED.std_dev_6m_annualized         , momentum_stats.std_dev_6m_annualized         ),
    price_return_6m             = COALESCE( EXCLUDED.price_return_6m               , momentum_stats.price_return_6m               ),
    mr6                         = COALESCE( EXCLUDED.mr6                           , momentum_stats.mr6                           );
""")
                .executeUpdate();

        entityManager.clear();
    }

    public MomentumZStats getMomentumZStatsForDate(LocalDate date, String tickerListName) {
        return momentumZStatsRepository.findByPriceDateAndTickerListName(date, tickerListName);
    }

    @Transactional
    public void saveMomentumZStats(List<MomentumZStats> momentumZStats) {
        if (momentumZStats == null || momentumZStats.isEmpty()) {
            return;
        }

        entityManager
                .createNativeQuery(
                        """
                            CREATE TEMP TABLE temp_momentum_z_stats (
                                ticker_list_name            VARCHAR(20) NOT NULL,
                                price_date                  DATE NOT NULL,
                                mean_mr12                   NUMERIC(18, 8),
                                std_dev_mr12                NUMERIC(18, 8),
                                mean_mr6                    NUMERIC(18, 8),
                                std_dev_mr6                 NUMERIC(18, 8)
                            ) ON COMMIT DROP;
                        """)
                .executeUpdate();

        Session session = entityManager.unwrap(Session.class);
        session.doWork(
                connection -> {
                    String sql =
                            "insert into temp_momentum_z_stats(ticker_list_name, price_date,"
                                + " mean_mr12, std_dev_mr12, mean_mr6, std_dev_mr6 ) values(?, ?,"
                                + " ?, ?, ?, ?)";
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        int batchSize = properties.getPersistenceJdbcBatchSize();
                        int count = 0;

                        for (MomentumZStats mzs : momentumZStats) {
                            ps.setString(1, mzs.getTickerListName());
                            ps.setDate(2, java.sql.Date.valueOf(mzs.getPriceDate()));
                            ps.setBigDecimal(3, mzs.getMeanMr12());
                            ps.setBigDecimal(4, mzs.getStdDevMr12());
                            ps.setBigDecimal(5, mzs.getMeanMr6());
                            ps.setBigDecimal(6, mzs.getStdDevMr6());
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
insert into nse.momentum_z_stats(ticker_list_name, price_date, mean_mr12, std_dev_mr12, mean_mr6, std_dev_mr6 )
select ticker_list_name, price_date, mean_mr12, std_dev_mr12, mean_mr6, std_dev_mr6
from temp_momentum_z_stats
    ON CONFLICT (ticker_list_name, price_date)
    DO UPDATE SET
        mean_mr12           = COALESCE( EXCLUDED.mean_mr12        , momentum_z_stats.mean_mr12        ),
        std_dev_mr12        = COALESCE( EXCLUDED.std_dev_mr12     , momentum_z_stats.std_dev_mr12     ),
        mean_mr6            = COALESCE( EXCLUDED.mean_mr6         , momentum_z_stats.mean_mr6         ),
        std_dev_mr6         = COALESCE( EXCLUDED.std_dev_mr6      , momentum_z_stats.std_dev_mr6      );
""")
                .executeUpdate();

        entityManager.clear();
    }

    @Transactional
    public void saveMomentumZStatsScores(List<MomentumZStatsScore> momentumZStatsScores) {
        if (momentumZStatsScores == null || momentumZStatsScores.isEmpty()) {
            return;
        }

        entityManager
                .createNativeQuery(
                        """
                            CREATE TEMP TABLE temp_momentum_z_stats_score (
                                 ticker_list_name            VARCHAR(20) NOT NULL,
                                 price_date                  DATE NOT NULL,
                                 ticker                      VARCHAR(20) NOT NULL,
                                 z12                         NUMERIC(18, 8),
                                 z6                          NUMERIC(18, 8),
                                 zw                          NUMERIC(18, 8),
                                 nmr                         NUMERIC(18, 8)
                            ) ON COMMIT DROP;
                        """)
                .executeUpdate();

        Session session = entityManager.unwrap(Session.class);
        session.doWork(
                connection -> {
                    String sql =
                            "INSERT INTO temp_momentum_z_stats_score ( ticker_list_name,"
                                + " price_date, ticker, z12, z6, zw, nmr  ) values( ?, ?, ?, ?, ?,"
                                + " ?, ?)";
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        int batchSize = properties.getPersistenceJdbcBatchSize();
                        int count = 0;

                        for (MomentumZStatsScore mzss : momentumZStatsScores) {
                            ps.setString(1, mzss.getTickerListName());
                            ps.setDate(2, java.sql.Date.valueOf(mzss.getPriceDate()));
                            ps.setString(3, mzss.getTicker());
                            ps.setBigDecimal(4, mzss.getZ12());
                            ps.setBigDecimal(5, mzss.getZ6());
                            ps.setBigDecimal(6, mzss.getZw());
                            ps.setBigDecimal(7, mzss.getNmr());
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
INSERT INTO nse.momentum_z_stats_score ( ticker_list_name, price_date, ticker, z12, z6, zw, nmr  )
    select ticker_list_name, price_date, ticker, z12, z6, zw, nmr
    from temp_momentum_z_stats_score
        ON CONFLICT (ticker_list_name, price_date, ticker)
        DO UPDATE SET
            z12 = COALESCE( EXCLUDED.z12 , momentum_z_stats_score.z12  ),
            z6  = COALESCE( EXCLUDED.z6  , momentum_z_stats_score.z6   ),
            zw  = COALESCE( EXCLUDED.zw  , momentum_z_stats_score.zw   ),
            nmr = COALESCE( EXCLUDED.nmr , momentum_z_stats_score.nmr  );
""")
                .executeUpdate();

        entityManager.clear();
    }
}
