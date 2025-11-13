package com.trading.robot.service;

import com.trading.robot.model.Trade;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects runtime metrics for trades.
 */
public class MetricsService {

    private final Map<String, Trade> trackedTrades = new ConcurrentHashMap<>();

    public void trackTrade(Trade trade) {
        trackedTrades.put(trade.getTradeId(), trade);
    }

    public long executionLatency(String tradeId) {
        Trade trade = trackedTrades.get(tradeId);
        if (trade == null) {
            throw new IllegalArgumentException("Trade not tracked: " + tradeId);
        }
        return trade.getExecutionLatencyMillis();
    }

    public int retryAttempts(String tradeId) {
        Trade trade = trackedTrades.get(tradeId);
        if (trade == null) {
            throw new IllegalArgumentException("Trade not tracked: " + tradeId);
        }
        return trade.getRetryCount();
    }
}

