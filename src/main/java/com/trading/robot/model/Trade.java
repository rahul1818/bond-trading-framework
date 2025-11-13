package com.trading.robot.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a trade and its lifecycle.
 */
public class Trade {

    private final String tradeId;
    private final Instrument instrument;
    private final Trader trader;
    private final int quantity;
    private final TradeSide side;
    private final BigDecimal limitPrice;
    private final Instant createdAt;
    private final List<TradeStateChange> stateHistory = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger filledQuantity = new AtomicInteger();
    private final AtomicReference<BigDecimal> executionPrice = new AtomicReference<>();
    private final AtomicInteger retryCount = new AtomicInteger();
    private volatile boolean duplicateSubmissionDetected;
    private volatile long executionLatencyMillis;

    public Trade(Instrument instrument,
                 Trader trader,
                 int quantity,
                 TradeSide side,
                 BigDecimal limitPrice) {
        this.tradeId = UUID.randomUUID().toString();
        this.instrument = instrument;
        this.trader = trader;
        this.quantity = quantity;
        this.side = side;
        this.limitPrice = limitPrice;
        this.createdAt = Instant.now();
        recordState(TradeState.CREATED, "Trade created");
    }

    public String getTradeId() {
        return tradeId;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public Trader getTrader() {
        return trader;
    }

    public int getQuantity() {
        return quantity;
    }

    public TradeSide getSide() {
        return side;
    }

    public BigDecimal getLimitPrice() {
        return limitPrice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<TradeStateChange> getStateHistory() {
        return Collections.unmodifiableList(stateHistory);
    }

    public void recordState(TradeState state, String reason) {
        stateHistory.add(new TradeStateChange(state, Instant.now(), reason));
    }

    public TradeState getCurrentState() {
        if (stateHistory.isEmpty()) {
            return null;
        }
        return stateHistory.get(stateHistory.size() - 1).getState();
    }

    public int getFilledQuantity() {
        return filledQuantity.get();
    }

    public void setFilledQuantity(int filledQuantity) {
        this.filledQuantity.set(filledQuantity);
    }

    public void incrementRetry() {
        retryCount.incrementAndGet();
    }

    public int getRetryCount() {
        return retryCount.get();
    }

    public BigDecimal getExecutionPrice() {
        return executionPrice.get();
    }

    public void setExecutionPrice(BigDecimal price) {
        executionPrice.set(price);
    }

    public boolean isDuplicateSubmissionDetected() {
        return duplicateSubmissionDetected;
    }

    public void setDuplicateSubmissionDetected(boolean duplicateSubmissionDetected) {
        this.duplicateSubmissionDetected = duplicateSubmissionDetected;
    }

    public long getExecutionLatencyMillis() {
        return executionLatencyMillis;
    }

    public void setExecutionLatencyMillis(long executionLatencyMillis) {
        this.executionLatencyMillis = executionLatencyMillis;
    }
}

