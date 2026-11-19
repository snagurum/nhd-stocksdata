ALTER TABLE nse.stock_stats ADD column IF NOT EXISTS return_1yr numeric(10,2);
ALTER TABLE nse.stock_stats ADD column IF NOT EXISTS return_6m numeric(10,2);
ALTER TABLE nse.stock_stats ADD column IF NOT EXISTS return_3m numeric(10,2);
ALTER TABLE nse.stock_stats ADD column IF NOT EXISTS return_1m numeric(10,2);

ALTER TABLE nse.stock_stats ADD column IF NOT EXISTS std_dev_1yr numeric(10,2);
ALTER TABLE nse.stock_stats ADD column IF NOT EXISTS std_dev_6m numeric(10,2);
ALTER TABLE nse.stock_stats ADD column IF NOT EXISTS std_dev_3m numeric(10,2);
ALTER TABLE nse.stock_stats ADD column IF NOT EXISTS std_dev_1m numeric(10,2);

