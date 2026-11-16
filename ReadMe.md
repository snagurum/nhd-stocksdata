# nhd-stocksdata

Spring Boot service owning NSE entities, repositories, JDBC upserts and migrations.
Runs at `http://localhost:8082/stocks`.

```powershell
mvn clean verify
mvn spring-boot:run
```

Database variables: `DB_HOST`, `DB_PORT`, `DB_DB`, `DB_USERNAME`, `DB_PWD`.
Defaults match nhd-processor. No rule engine or job-runner code is included.
Active ticker membership still reads loader-owned `lt.stocks`, preserving existing behavior.

## HTTP API

All paths below are relative to `/stocks`. Internal calls use POST with JSON query bodies to avoid URL length limits for ticker batches.

| Path | Body / result |
| --- | --- |
| `/internal/{resource}/batch` | Array of model objects; upserts and returns HTTP 200 only after commit |
| `/internal/{stock-prices,stock-stats,momentum-stats}/date` | DataQuery; returns `{content,totalElements}` for the exact date |
| `/internal/{stock-prices,stock-stats}/ticker` | DataQuery; returns a page of ticker history from the optional date |
| `/internal/{stock-prices,stock-stats}/tickers` | DataQuery; returns history grouped by ticker |
| `/internal/momentum-z-stats/date` | DataQuery with date and tickerListName; returns `{value: object-or-null}` |
| `/internal/active-tickers` | `{}`; returns active ticker strings |
| GET `/api/v1/stockStats?fromDate=2026-01-01&tickers=TCS,INFY` | Existing public stats API moved from processor; existing exact-date and ticker-prefix semantics preserved |

Resources for batch writes: `stock-prices`, `stock-stats`, `momentum-stats`, `momentum-z-stats`, `momentum-z-scores`.
DataQuery fields: `date`, `tickers`, `ticker`, `tickerListName`, `page` (zero based), `size` (1–10000).
The processor splits writes into configurable HTTP batches (default 2000). Each batch is its own transaction. A failed request stops the job; retrying already committed batches uses existing upserts. Network timeouts have an uncertain commit outcome, so retries must remain upserts.

## Database schema

Stocksdata uses `nse` for Hibernate's default schema, Liquibase's default schema, and Liquibase history/lock tables. NSE entity mappings and SQL migrations also explicitly target `nse`. Active ticker lookup continues to read loader-owned `lt.stocks`.

## Upgrade an existing database

1. Stop processor jobs and both services, then back up the database.
2. If migration history was previously maintained by processor, run `../nhd-processor/ops/migrate-stocksdata-liquibase-history.sql` once before starting stocksdata. This deployment helper copies only the extracted NSE history into `nse.databasechangelog`, retaining IDs, authors, paths and checksums.
3. Start stocksdata, then processor. Set processor `STOCKSDATA_URL` if needed (default `http://localhost:8082/stocks`).
4. Stock-data consumers must use `/stocks/api/v1/stockStats` on port 8082.

The extracted SQL files retain their original changelog paths. Do not start against existing NSE tables with an empty migration history or clear checksums to bypass a mismatch.

For a fresh database, provision the `nse` schema first; skip the history-copy script and start stocksdata normally. Provision loader schema/data separately for active ticker lookup.
Stocksdata needs NSE access and SELECT on loader-owned `lt.stocks`. It has no dependency on processor's schema. No database migration is run by the unit/HTTP tests.
