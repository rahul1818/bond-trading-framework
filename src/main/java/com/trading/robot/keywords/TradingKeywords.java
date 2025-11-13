package com.trading.robot.keywords;

import com.trading.robot.model.MarketQuote;
import com.trading.robot.model.Trade;
import com.trading.robot.model.TradeRequest;
import com.trading.robot.model.TradeSide;
import com.trading.robot.model.TradeState;
import com.trading.robot.model.ValidationResult;
import com.trading.robot.service.TradingEngine;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;

@RobotKeywords
public class TradingKeywords {

    private final TradingEngine engine = new TradingEngine();
    private final Map<String, List<MarketQuote>> capturedQuotes = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @RobotKeyword("Create trade and return trade identifier")
    public String createTrade(String isin, String trader, int quantity, String side, double limitPrice) {
        TradeRequest request = new TradeRequest(
            isin,
            trader,
            quantity,
            TradeSide.valueOf(side.toUpperCase(Locale.ROOT)),
            BigDecimal.valueOf(limitPrice)
        );
        Trade trade = engine.createTrade(request);
        return trade.getTradeId();
    }

    @RobotKeyword("Execute trade without retries")
    public void executeTrade(String tradeId) {
        engine.executeTrade(tradeId);
    }

    @RobotKeyword("Execute trade with retry and exponential backoff")
    public void retryExecutionWithBackoff(String tradeId, int maxAttempts) {
        engine.executeWithRetry(tradeId, maxAttempts);
    }

    @RobotKeyword("Confirm trade using tolerance in basis points")
    public void confirmTrade(String tradeId, double toleranceBps) {
        engine.confirmTrade(tradeId, toleranceBps);
    }

    @RobotKeyword("Cancel trade with provided reason")
    public void cancelTrade(String tradeId, String reason) {
        engine.cancelTrade(tradeId, reason);
    }

    @RobotKeyword("Subscribe to market feed updates for an ISIN")
    public void subscribeToMarketFeed(String isin) {
        List<MarketQuote> buffer = capturedQuotes.computeIfAbsent(isin, key -> new CopyOnWriteArrayList<>());
        engine.subscribeMarket(isin, quote -> capturedQuotes.computeIfAbsent(isin, key -> new CopyOnWriteArrayList<>()).add(quote));
        MarketQuote snapshot = engine.latestQuote(isin);
        if (snapshot != null) {
            buffer.add(snapshot);
        }
    }

    @RobotKeyword("Return captured market updates for an ISIN")
    public List<Map<String, Object>> getCapturedMarketUpdates(String isin) {
        return capturedQuotes.getOrDefault(isin, Collections.emptyList()).stream()
            .map(this::toMap)
            .collect(Collectors.toList());
    }

    @RobotKeyword("Validate trader exposure against projected notional")
    public boolean validateTraderExposure(String trader, double projectedNotional) {
        ValidationResult result = engine.validateExposure(trader, BigDecimal.valueOf(projectedNotional));
        if (!result.isValid()) {
            throw new IllegalStateException(result.getMessage());
        }
        return true;
    }

    @RobotKeyword("Assert trade state history matches expected states (comma separated)")
    public void assertTradeStateHistory(String tradeId, String expectedStates) {
        List<TradeState> actual = engine.history(tradeId);
        List<TradeState> expected = Arrays.stream(expectedStates.split(","))
            .map(String::trim)
            .filter(token -> !token.isEmpty())
            .map(token -> TradeState.valueOf(token.toUpperCase(Locale.ROOT)))
            .collect(Collectors.toList());
        if (!actual.equals(expected)) {
            throw new AssertionError("Expected states " + expected + " but was " + actual);
        }
    }

    @RobotKeyword("Return trade state history as list")
    public List<String> getTradeStateHistory(String tradeId) {
        return engine.history(tradeId).stream().map(Enum::name).collect(Collectors.toList());
    }

    @RobotKeyword("Compare execution price to market average and return deviation in BPS")
    public double compareExecutionPriceToMarketAverage(String tradeId) {
        return engine.compareExecutionToAverage(tradeId);
    }

    @RobotKeyword("Load trade requests from CSV file and return as dictionaries")
    public List<Map<String, Object>> loadTradesFromCsv(String csvAbsolutePath) {
        List<TradeRequest> requests = engine.loadTradesFromCsv(csvAbsolutePath);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TradeRequest request : requests) {
            rows.add(Map.of(
                "tradeId", "",
                "isin", request.getIsin(),
                "trader", request.getTraderName(),
                "quantity", request.getQuantity(),
                "side", request.getSide().name(),
                "limitPrice", request.getLimitPrice().doubleValue()
            ));
        }
        return rows;
    }

    @RobotKeyword("Start market data feed for provided ISINs")
    public void startMarketDataFeed(Collection<String> isins) {
        engine.startFeed(isins);
    }

    @RobotKeyword("Return latest quote for ISIN as dictionary")
    public Map<String, Object> getLatestQuote(String isin) {
        MarketQuote quote = engine.latestQuote(isin);
        return toMap(quote);
    }

    @RobotKeyword("Return execution latency for trade in millis")
    public long getExecutionLatencyMillis(String tradeId) {
        return engine.executionLatency(tradeId);
    }

    @RobotKeyword("Return retry attempts count for trade")
    public int getRetryAttempts(String tradeId) {
        return engine.retryAttempts(tradeId);
    }

    @RobotKeyword("Apply market shock percentage to instrument")
    public void applyMarketShock(String isin, double percentChange) {
        engine.applyMarketShock(isin, percentChange);
    }

    @RobotKeyword("Inject execution failure for trade to test retry resilience")
    public void injectExecutionFailure(String tradeId) {
        engine.injectExecutionFailure(tradeId);
    }

    @RobotKeyword("Force partial fill quantity for trade before execution")
    public void forcePartialFill(String tradeId, int filledQuantity) {
        engine.forcePartialFill(tradeId, filledQuantity);
    }

    @RobotKeyword("Execute trades concurrently")
    public void executeTradesConcurrently(List<String> tradeIds) {
        List<CompletableFuture<Void>> futures = tradeIds.stream()
            .map(tradeId -> CompletableFuture.runAsync(() -> engine.executeTrade(tradeId), executor))
            .collect(Collectors.toList());
        futures.forEach(CompletableFuture::join);
    }

    @RobotKeyword("Confirm trades sequentially with tolerance")
    public void confirmTrades(List<String> tradeIds, double toleranceBps) {
        for (String tradeId : tradeIds) {
            engine.confirmTrade(tradeId, toleranceBps);
        }
    }

    @RobotKeyword("Get trade details as dictionary")
    public Map<String, Object> getTradeDetails(String tradeId) {
        Trade trade = engine.getTrade(tradeId);
        return Map.of(
            "tradeId", trade.getTradeId(),
            "isin", trade.getInstrument().getIsin(),
            "trader", trade.getTrader().getName(),
            "quantity", trade.getQuantity(),
            "filledQuantity", trade.getFilledQuantity(),
            "side", trade.getSide().name(),
            "limitPrice", trade.getLimitPrice().doubleValue(),
            "executionPrice", Optional.ofNullable(trade.getExecutionPrice()).map(BigDecimal::doubleValue).orElse(null),
            "duplicate", trade.isDuplicateSubmissionDetected()
        );
    }

    public String ping() {
        return "pong";
    }

    @RobotKeyword("Shutdown trading engine and release resources")
    public void shutdownTradingEngine() {
        engine.shutdown();
        executor.shutdownNow();
    }

    private Map<String, Object> toMap(MarketQuote quote) {
        if (quote == null) {
            return Collections.emptyMap();
        }
        return Map.of(
            "isin", quote.getIsin(),
            "bid", quote.getBid().doubleValue(),
            "ask", quote.getAsk().doubleValue(),
            "mid", quote.getMid().doubleValue(),
            "timestamp", quote.getTimestamp().toString()
        );
    }
}

