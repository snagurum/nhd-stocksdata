

-- MOMENTUM Z STATS SCORE

CREATE TABLE IF NOT EXISTS nse.momentum_z_stats_score (
    id                          INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    ticker_list_name            VARCHAR(20) NOT NULL,
    price_date                  DATE NOT NULL,
    ticker                      VARCHAR(20) NOT NULL,
    z12                         NUMERIC(10, 2),
    z6                          NUMERIC(10, 2),
    zw                          NUMERIC(10, 2),    -- Weighted average Z-score , Z_w = 0.5 * Z_12 + 0.5 * Z_6
    nmr                         NUMERIC(10, 2)     -- Normalized Momentum Score, S = 1 + Z_w [ Z_w >= 0 ], 1/(1 - Z_w) [ Z_w < 0 ]  
);


DO '
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_constraint
        WHERE conrelid = ''nse.momentum_z_stats_score''::regclass
          AND conname = ''mzss_unique_tickerlist_date_ticker''
    ) THEN
        ALTER TABLE nse.momentum_z_stats_score ADD CONSTRAINT mzss_unique_tickerlist_date_ticker UNIQUE (ticker_list_name, price_date,ticker);
    END IF;
END;';

CREATE INDEX IF NOT EXISTS idx_momentum_z_stats_score_ticker_date
ON nse.momentum_z_stats_score (ticker_list_name, price_date, ticker);

CREATE INDEX IF NOT EXISTS idx_momentum_z_stats_score_ticker_list_name_date
ON nse.momentum_z_stats_score (ticker_list_name, price_date);

CREATE INDEX IF NOT EXISTS idx_momentum_z_stats_score_date
ON nse.momentum_z_stats_score ( price_date);

