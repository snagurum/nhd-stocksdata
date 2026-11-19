ALTER TABLE nse.stock_stats
    ADD COLUMN IF NOT EXISTS return_2_volatility NUMERIC(18,8),
    ADD COLUMN IF NOT EXISTS drop_percentage NUMERIC(18,8);
