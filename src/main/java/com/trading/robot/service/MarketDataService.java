package com.trading.robot.service;

import com.trading.robot.model.MarketQuote;
import com.trading.robot.util.RandomUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simulates a real-time market data feed with asynchronous updates and random disruptions.
 */
public class MarketDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketDataService.class);
    private static final int HISTORY_WINDOW = 50;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
    private final Map<String, MarketQuote> latestQuotes = new ConcurrentHashMap<>();
    private final Map<String, Deque<BigDecimal>> midHistory = new ConcurrentHashMap<>();
    private final Map<String, List<Consumer<MarketQuote>>> subscribers = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public void startFeed(Collection<String> isins) {
        isins.forEach(this::startStreaming);
    }

    public void startStreaming(String isin) {
        publishTick(isin);
        ScheduledFuture<?> task = scheduledTasks.computeIfAbsent(isin, key ->
            executor.scheduleAtFixedRate(() -> publishTick(isin), 800, 800, TimeUnit.MILLISECONDS)
        );
        if (task.isCancelled()) {
            publishTick(isin);
            scheduledTasks.put(isin, executor.scheduleAtFixedRate(() -> publishTick(isin), 800, 800, TimeUnit.MILLISECONDS));
        }
    }

    private void publishTick(String isin) {
        try {
            if (RandomUtils.chance(0.05)) {
                LOGGER.warn("Network jitter detected for {}, skipping tick", isin);
                return;
            }
            MarketQuote previous = latestQuotes.getOrDefault(isin,
                new MarketQuote(isin, BigDecimal.valueOf(99.5), BigDecimal.valueOf(100.5), Instant.now()));
            double shockFactor = RandomUtils.chance(0.1) ? RandomUtils.randomInt(-50, 50) / 10_000d : 0;
            BigDecimal base = previous.getMid().multiply(BigDecimal.valueOf(1 + shockFactor));
            BigDecimal bid = base.subtract(BigDecimal.valueOf(0.05)).setScale(4, RoundingMode.HALF_UP);
            BigDecimal ask = base.add(BigDecimal.valueOf(0.05)).setScale(4, RoundingMode.HALF_UP);
            MarketQuote quote = new MarketQuote(isin, bid, ask, Instant.now());
            latestQuotes.put(isin, quote);
            midHistory.computeIfAbsent(isin, key -> new ArrayDeque<>()).addLast(quote.getMid());
            Deque<BigDecimal> history = midHistory.get(isin);
            while (history.size() > HISTORY_WINDOW) {
                history.removeFirst();
            }
            notifySubscribers(isin, quote);
        } catch (Exception ex) {
            LOGGER.error("Failed to publish tick for {}", isin, ex);
        }
    }

    private void notifySubscribers(String isin, MarketQuote quote) {
        subscribers.computeIfAbsent(isin, key -> new CopyOnWriteArrayList<>())
            .forEach(subscriber -> {
                try {
                    subscriber.accept(quote);
                } catch (Exception ex) {
                    LOGGER.error("Subscriber handling failed for {}", isin, ex);
                }
            });
    }

    public void subscribe(String isin, Consumer<MarketQuote> consumer) {
        subscribers.computeIfAbsent(isin, key -> new CopyOnWriteArrayList<>()).add(consumer);
        MarketQuote snapshot = latestQuotes.get(isin);
        if (snapshot != null) {
            try {
                consumer.accept(snapshot);
            } catch (Exception ex) {
                LOGGER.error("Immediate dispatch failed for {}", isin, ex);
            }
        }
    }

    public Optional<MarketQuote> latestQuote(String isin) {
        MarketQuote quote = latestQuotes.computeIfAbsent(isin, key -> {
            MarketQuote seed = new MarketQuote(key, BigDecimal.valueOf(99.5), BigDecimal.valueOf(100.5), Instant.now());
            midHistory.computeIfAbsent(key, historyKey -> new ArrayDeque<>()).addLast(seed.getMid());
            return seed;
        });
        return Optional.ofNullable(quote);
    }

    public BigDecimal averageMid(String isin) {
        Deque<BigDecimal> history = midHistory.getOrDefault(isin, new ArrayDeque<>());
        if (history.isEmpty()) {
            throw new IllegalStateException("No market data available for " + isin);
        }
        BigDecimal sum = history.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(history.size()), 6, RoundingMode.HALF_UP);
    }

    public List<BigDecimal> recentMids(String isin) {
        return new ArrayList<>(midHistory.getOrDefault(isin, new ArrayDeque<>()));
    }

    public void applyShock(String isin, double percentChange) {
        MarketQuote current = latestQuotes.get(isin);
        if (current == null) {
            throw new IllegalArgumentException("Cannot shock unknown instrument " + isin);
        }
        BigDecimal factor = BigDecimal.valueOf(1 + percentChange / 100d);
        BigDecimal bid = current.getBid().multiply(factor).setScale(4, RoundingMode.HALF_UP);
        BigDecimal ask = current.getAsk().multiply(factor).setScale(4, RoundingMode.HALF_UP);
        MarketQuote shocked = new MarketQuote(isin, bid, ask, Instant.now());
        latestQuotes.put(isin, shocked);
        midHistory.computeIfAbsent(isin, key -> new ArrayDeque<>()).addLast(shocked.getMid());
        notifySubscribers(isin, shocked);
    }

    public void stop() {
        scheduledTasks.values().forEach(future -> future.cancel(true));
        executor.shutdownNow();
    }
}

