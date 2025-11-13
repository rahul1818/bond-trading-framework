package com.trading.robot.config;

import com.trading.robot.model.Trade;
import com.trading.robot.model.Trader;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory data store for trades and traders.
 */
public class MockDatabase {

    private final Map<String, Trade> trades = new ConcurrentHashMap<>();
    private final Map<String, Trader> traders = new ConcurrentHashMap<>();

    public MockDatabase() {
        bootstrapTraders();
    }

    private void bootstrapTraders() {
        registerTrader(new Trader("Alice", BigDecimal.valueOf(250_000_000), BigDecimal.valueOf(150_000_000)));
        registerTrader(new Trader("Bob", BigDecimal.valueOf(150_000_000), BigDecimal.valueOf(80_000_000)));
        registerTrader(new Trader("Charlie", BigDecimal.valueOf(180_000_000), BigDecimal.valueOf(100_000_000)));
        registerTrader(new Trader("Diana", BigDecimal.valueOf(220_000_000), BigDecimal.valueOf(120_000_000)));
    }

    public void registerTrader(Trader trader) {
        traders.putIfAbsent(trader.getName(), trader);
    }

    public Optional<Trader> findTrader(String name) {
        return Optional.ofNullable(traders.get(name));
    }

    public Collection<Trader> getAllTraders() {
        return traders.values();
    }

    public void saveTrade(Trade trade) {
        trades.put(trade.getTradeId(), trade);
    }

    public Optional<Trade> findTrade(String tradeId) {
        return Optional.ofNullable(trades.get(tradeId));
    }

    public Collection<Trade> getTrades() {
        return trades.values();
    }

    public void clear() {
        trades.clear();
    }
}

