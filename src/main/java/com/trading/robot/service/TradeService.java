package com.trading.robot.service;

import com.trading.robot.config.MockDatabase;
import com.trading.robot.model.Instrument;
import com.trading.robot.model.MarketQuote;
import com.trading.robot.model.Trade;
import com.trading.robot.model.TradeException;
import com.trading.robot.model.TradeRequest;
import com.trading.robot.model.TradeSide;
import com.trading.robot.model.TradeState;
import com.trading.robot.model.TradeStateChange;
import com.trading.robot.model.Trader;
import com.trading.robot.model.ValidationResult;
import com.trading.robot.util.RandomUtils;
import com.trading.robot.util.RetryPolicy;
import com.trading.robot.util.TimeUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages trade lifecycle transitions and resiliency logic.
 */
public class TradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TradeService.class);
    private static final int CREDIT_TIMEOUT_SECONDS = 5;

    private final MockDatabase database;
    private final InstrumentService instrumentService;
    private final MarketDataService marketDataService;
    private final CreditExposureService creditExposureService;
    private final ConcurrentHashMap<String, Lock> tradeLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> executionFailureToggle = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> partialFillOverrides = new ConcurrentHashMap<>();

    public TradeService(MockDatabase database,
                        InstrumentService instrumentService,
                        MarketDataService marketDataService,
                        CreditExposureService creditExposureService) {
        this.database = database;
        this.instrumentService = instrumentService;
        this.marketDataService = marketDataService;
        this.creditExposureService = creditExposureService;
    }

    public Trade createTrade(TradeRequest request) {
        Instrument instrument = instrumentService.findInstrument(request.getIsin())
            .orElseThrow(() -> new TradeException("Instrument not found for ISIN " + request.getIsin()));
        Trader trader = database.findTrader(request.getTraderName())
            .orElseThrow(() -> new TradeException("Trader not registered: " + request.getTraderName()));
        ValidationResult validation = validateTradeLimits(trader, request.getQuantity(), request.getLimitPrice());
        if (!validation.isValid()) {
            throw new TradeException(validation.getMessage());
        }
        BigDecimal projected = trader.getCurrentExposure()
            .add(request.getLimitPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        boolean validExposure = invokeCreditService(trader.getName(), projected);
        if (!validExposure) {
            throw new TradeException("Credit exposure validation failed for trader " + trader.getName());
        }
        Trade trade = new Trade(instrument, trader, request.getQuantity(), request.getSide(), request.getLimitPrice());
        database.saveTrade(trade);
        LOGGER.info("Trade {} created for trader {} on {}", trade.getTradeId(), trader.getName(), instrument.getIsin());
        if (RandomUtils.chance(0.03)) {
            trade.setDuplicateSubmissionDetected(true);
            trade.recordState(TradeState.RETRY, "Duplicate submission detected");
        }
        return trade;
    }

    private boolean invokeCreditService(String traderName, BigDecimal projected) {
        CompletableFuture<Boolean> future = creditExposureService.validateExposureAsync(traderName, projected);
        try {
            return future.get(CREDIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TradeException("Credit exposure validation interrupted", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new TradeException("Credit exposure service failure", e);
        }
    }

    private static ValidationResult validateTradeLimits(Trader trader, int quantity, BigDecimal limitPrice) {
        if (quantity <= 0 || limitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.error("Quantity and limit price must be positive");
        }
        BigDecimal tradeNotional = limitPrice.multiply(BigDecimal.valueOf(quantity));
        if (tradeNotional.compareTo(trader.getTradeLimit()) > 0) {
            return ValidationResult.error("Trade limit exceeded for trader " + trader.getName());
        }
        return ValidationResult.ok("Trade limits validated");
    }

    public ValidationResult validateExposure(String traderName, BigDecimal projectedNotional) {
        Trader trader = database.findTrader(traderName)
            .orElseThrow(() -> new TradeException("Trader not registered: " + traderName));
        boolean valid = invokeCreditService(traderName, trader.getCurrentExposure().add(projectedNotional));
        return valid ? ValidationResult.ok("Exposure within limits") :
            ValidationResult.error("Exposure breach for trader " + traderName);
    }

    public Trade executeTrade(String tradeId) {
        Trade trade = getTradeOrThrow(tradeId);
        Lock lock = tradeLocks.computeIfAbsent(tradeId, key -> new ReentrantLock());
        lock.lock();
        try {
            TradeState currentState = trade.getCurrentState();
            if (currentState == TradeState.CONFIRMED || currentState == TradeState.CANCELLED || currentState == TradeState.REJECTED) {
                throw new TradeException("Trade " + tradeId + " is already terminal in state " + currentState);
            }
            if (currentState == TradeState.EXECUTED || currentState == TradeState.PARTIALLY_FILLED || currentState == TradeState.PENDING_CONFIRMATION) {
                throw new TradeException("Trade " + tradeId + " is already executed");
            }
            Instant start = Instant.now();
            if (Boolean.TRUE.equals(executionFailureToggle.remove(tradeId))) {
                trade.recordState(TradeState.RETRY, "Injected failure during execution");
                throw new TradeException("Injected execution failure for trade " + tradeId);
            }
            if (RandomUtils.chance(0.05)) {
                trade.recordState(TradeState.RETRY, "Network hiccup during execution");
                throw new TradeException("Execution failed due to network hiccup");
            }
            MarketQuote quote = marketDataService.latestQuote(trade.getInstrument().getIsin())
                .orElseThrow(() -> new TradeException("No market data available for execution"));

            BigDecimal price = RandomUtils.randomPrice(quote.getMid(), 15);
            trade.setExecutionPrice(price);
            trade.recordState(TradeState.EXECUTED, "Executed at " + price);

            int filled = trade.getQuantity();
            Integer override = partialFillOverrides.remove(tradeId);
            if (override != null) {
                filled = Math.min(Math.max(override, 1), trade.getQuantity());
                if (filled < trade.getQuantity()) {
                    trade.recordState(TradeState.PARTIALLY_FILLED, "Partial fill override of " + filled);
                }
            } else if (RandomUtils.chance(0.35)) {
                filled = Math.max(1, (int) (trade.getQuantity() * RandomUtils.randomInt(50, 95) / 100.0));
                trade.recordState(TradeState.PARTIALLY_FILLED, "Partial fill of " + filled);
            }
            trade.setFilledQuantity(filled);
            BigDecimal notional = price.multiply(BigDecimal.valueOf(filled));
            trade.getTrader().addExposure(notional);
            trade.recordState(TradeState.PENDING_CONFIRMATION, "Awaiting confirmation");
            trade.setExecutionLatencyMillis(TimeUtils.betweenMillis(start, Instant.now()));
            LOGGER.info("Trade {} executed at {} with fill {}", tradeId, price, trade.getFilledQuantity());
            return trade;
        } finally {
            lock.unlock();
        }
    }

    public Trade executeWithRetry(String tradeId, RetryPolicy retryPolicy) {
        return retryPolicy.execute(() -> {
            Trade trade = executeTrade(tradeId);
            trade.incrementRetry();
            return trade;
        });
    }

    public Trade confirmTrade(String tradeId, double toleranceBps) {
        Trade trade = getTradeOrThrow(tradeId);
        Lock lock = tradeLocks.computeIfAbsent(tradeId, key -> new ReentrantLock());
        lock.lock();
        try {
            TradeState currentState = trade.getCurrentState();
            if (currentState == TradeState.CONFIRMED || currentState == TradeState.REJECTED || currentState == TradeState.CANCELLED) {
                throw new TradeException("Trade " + tradeId + " already confirmed outcome: " + currentState);
            }
            if (currentState != TradeState.PENDING_CONFIRMATION && currentState != TradeState.PARTIALLY_FILLED) {
                throw new TradeException("Trade " + tradeId + " not ready for confirmation (state " + currentState + ")");
            }
            MarketQuote latest = marketDataService.latestQuote(trade.getInstrument().getIsin())
                .orElseThrow(() -> new TradeException("Market data unavailable for confirmation"));
            BigDecimal avg = marketDataService.averageMid(trade.getInstrument().getIsin());
            double deviation = deviationBps(trade.getExecutionPrice(), avg);
            if (deviation > toleranceBps) {
                trade.recordState(TradeState.REJECTED, "Price deviation exceeded tolerance");
                trade.getTrader().reduceExposure(trade.getExecutionPrice().multiply(BigDecimal.valueOf(trade.getFilledQuantity())));
            } else if (RandomUtils.chance(0.1)) {
                trade.recordState(TradeState.CANCELLED, "Cancelled during confirmation due to market shock");
                trade.getTrader().reduceExposure(trade.getExecutionPrice().multiply(BigDecimal.valueOf(trade.getFilledQuantity())));
            } else {
                trade.recordState(TradeState.CONFIRMED, "Confirmed at " + latest.getMid());
            }
            return trade;
        } finally {
            lock.unlock();
        }
    }

    public Trade cancelTrade(String tradeId, String reason) {
        Trade trade = getTradeOrThrow(tradeId);
        trade.recordState(TradeState.CANCELLED, reason);
        if (trade.getExecutionPrice() != null) {
            trade.getTrader().reduceExposure(trade.getExecutionPrice().multiply(BigDecimal.valueOf(trade.getFilledQuantity())));
        }
        return trade;
    }

    public List<TradeState> getStateHistory(String tradeId) {
        Trade trade = getTradeOrThrow(tradeId);
        return trade.getStateHistory().stream().map(TradeStateChange::getState).collect(Collectors.toList());
    }

    public Trade getTradeOrThrow(String tradeId) {
        return database.findTrade(tradeId).orElseThrow(() -> new TradeException("Trade not found " + tradeId));
    }

    public void injectExecutionFailure(String tradeId) {
        executionFailureToggle.put(tradeId, true);
    }

    public void enforcePartialFill(String tradeId, int filledQuantity) {
        partialFillOverrides.put(tradeId, filledQuantity);
    }

    private static double deviationBps(BigDecimal executionPrice, BigDecimal averagePrice) {
        BigDecimal diff = executionPrice.subtract(averagePrice).abs();
        BigDecimal ratio = diff.divide(averagePrice, 6, RoundingMode.HALF_UP);
        return ratio.multiply(BigDecimal.valueOf(10_000)).doubleValue();
    }
}

