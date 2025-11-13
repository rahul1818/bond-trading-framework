## Test Scenario Catalogue

| # | Scenario | Description | Key Keywords | Validations |
|---|----------|-------------|--------------|-------------|
| 1 | Create and confirm valid trade | Happy-path lifecycle using CSV-driven trade | `Create Trade`, `Execute Trade And Confirm` | State history includes `CONFIRMED`, price deviation under threshold |
| 2 | Invalid ISIN or instrument | Validation failure on malformed ISIN | `Create Trade` with invalid input | Robot expects error message |
| 3 | Price deviation > tolerance | Force market shock before confirmation | `Apply Market Shock`, `Confirm Trade` | Trade state transitions to `REJECTED` |
| 4 | Execute already confirmed trade | Guard against duplicate execution | `Execute Trade And Confirm`, `Execute Trade` | Second execution raises exception |
| 5 | Simulated system failure | Inject execution failure to exercise retry logic | `Inject Execution Failure`, `Retry Execution With Backoff` | State history contains `RETRY` |
| 6 | Partial fill execution | Override fill quantity to enforce partial fills | `Force Partial Fill`, `Execute Trade` | Filled quantity < requested and state contains `PARTIALLY_FILLED` |
| 7 | Trade cancelled before confirmation | Cancel after execution but before confirmation | `Cancel Trade` | State history includes `CANCELLED` |
| 8 | Multiple concurrent traders | Parallel execution for several traders | `Execute Trades Concurrently`, `Confirm Trades` | No keyword failures; exposures validated |
| 9 | Rapid market update | Verify live feed sends ticks | `Get Captured Market Updates` | Market updates list length > threshold |
|10 | Risk breach attempt | Attempt trade exceeding trader limits | `Create Trade` | Error message indicates limit breach |

### Metrics

- Retry attempts and execution latency captured via Java model and exposed as Robot keywords for future assertions.
- Allure listener aggregates per-test step details and attaches timing metadata.

