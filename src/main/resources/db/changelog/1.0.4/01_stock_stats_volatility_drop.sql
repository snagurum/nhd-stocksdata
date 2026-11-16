ALTER TABLE nse.stock_stats
    ADD COLUMN return_2_volatility NUMERIC(18,8),
    ADD COLUMN drop_percentage NUMERIC(18,8);
