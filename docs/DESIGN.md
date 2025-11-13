## Design Notes

### Thread Safety

- `ConcurrentHashMap` and per-trade `ReentrantLock` guarantee safe lifecycle transitions.
- Market feed uses `ScheduledExecutorService` and `CopyOnWriteArrayList` for listener fan-out.
- Retry policy leverages blocking sleep with exponential backoff to simulate latency.

### State Management

- `Trade` entity records immutable `TradeStateChange` entries capturing reasons and timestamps.
- Partial fills and retries are persisted for later assertions through Robot keywords.

### Fault Injection

- Deterministic failure injection (`injectExecutionFailure`) and partial fill overrides allow reliable test coverage.
- Market shocks simulate volatility while maintaining consistent behaviour for confirmation logic.

### Logging & Observability

- `Logback` configuration logs to STDOUT; log level can be overridden by system property.
- Metrics service exposes execution latency and retry counts, enabling future dashboards.

### Extensibility

- Additional channels (e.g. FIX, WebSocket mocks) can be introduced by adding new services and keywords.
- Keyword library centralises Robot integration, keeping suites declarative.

