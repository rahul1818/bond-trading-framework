package com.trading.robot.service;

import com.trading.robot.config.MockDatabase;
import com.trading.robot.model.Trade;
import com.trading.robot.model.TradeRequest;
import com.trading.robot.util.RetryPolicy;
import java.time.Duration;
import java.util.Collection;
import java.util.List;

/**
 * High-level orchestrator bundling services used by Robot keywords.
 */
public class TradingEngine {

    private final MockDatabase database = new MockDatabase();
    private final InstrumentService instrumentService = new InstrumentService();
    private final MarketDataService marketDataService = new MarketDataService();
    private final CreditExposureService creditExposureService = new CreditExposureService(database);
    private final TradeService tradeService = new TradeService(database, instrumentService, marketDataService, creditExposureService);
    private final TradeDataLoader tradeDataLoader = new TradeDataLoader();
    private final MetricsService metricsService = new MetricsService();

    public Trade createTrade(TradeRequest request) {
        Trade trade = tradeService.createTrade(request);
        metricsService.trackTrade(trade);
        return trade;
    }

    public Trade executeTrade(String tradeId) {
        Trade trade = tradeService.executeTrade(tradeId);
        metricsService.trackTrade(trade);
        return trade;
    }

    public Trade executeWithRetry(String tradeId, int maxAttempts) {
        RetryPolicy policy = new RetryPolicy(maxAttempts, Duration.ofMillis(200), 1.5);
        Trade trade = tradeService.executeWithRetry(tradeId, policy);
        metricsService.trackTrade(trade);
        return trade;
    }

    public Trade confirmTrade(String tradeId, double toleranceBps) {
        return tradeService.confirmTrade(tradeId, toleranceBps);
    }

    public Trade cancelTrade(String tradeId, String reason) {
        return tradeService.cancelTrade(tradeId, reason);
    }

    public com.trading.robot.model.ValidationResult validateExposure(String trader, java.math.BigDecimal projectedNotional) {
        return tradeService.validateExposure(trader, projectedNotional);
    }

    public List<TradeRequest> loadTradesFromCsv(String path) {
        return tradeDataLoader.loadCsv(java.nio.file.Path.of(path));
    }

    public void subscribeMarket(String isin, java.util.function.Consumer<com.trading.robot.model.MarketQuote> consumer) {
        marketDataService.subscribe(isin, consumer);
        marketDataService.startStreaming(isin);
    }

    public void startFeed(Collection<String> isins) {
        marketDataService.startFeed(isins);
    }

    public com.trading.robot.model.MarketQuote latestQuote(String isin) {
        return marketDataService.latestQuote(isin).orElseThrow(() -> new IllegalStateException("No quotes for " + isin));
    }

    public double compareExecutionToAverage(String tradeId) {
        Trade trade = tradeService.getTradeOrThrow(tradeId);
        java.math.BigDecimal avg = marketDataService.averageMid(trade.getInstrument().getIsin());
        java.math.BigDecimal exec = trade.getExecutionPrice();
        java.math.BigDecimal deviation = exec.subtract(avg).abs();
        java.math.BigDecimal ratio = deviation.divide(avg, java.math.MathContext.DECIMAL64);
        return ratio.multiply(java.math.BigDecimal.valueOf(10_000)).doubleValue();
    }

    public List<com.trading.robot.model.TradeState> history(String tradeId) {
        return tradeService.getStateHistory(tradeId);
    }

    public long executionLatency(String tradeId) {
        return metricsService.executionLatency(tradeId);
    }

    public int retryAttempts(String tradeId) {
        return metricsService.retryAttempts(tradeId);
    }

    public Trade getTrade(String tradeId) {
        return tradeService.getTradeOrThrow(tradeId);
    }

    public void applyMarketShock(String isin, double percentChange) {
        marketDataService.applyShock(isin, percentChange);
    }

    public void injectExecutionFailure(String tradeId) {
        tradeService.injectExecutionFailure(tradeId);
    }

    public void forcePartialFill(String tradeId, int filledQuantity) {
        tradeService.enforcePartialFill(tradeId, filledQuantity);
    }

    public void shutdown() {
        creditExposureService.shutdown();
        marketDataService.stop();
    }
}

