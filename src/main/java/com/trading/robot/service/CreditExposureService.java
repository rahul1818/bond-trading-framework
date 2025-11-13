package com.trading.robot.service;

import com.trading.robot.config.MockDatabase;
import com.trading.robot.model.Trader;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock asynchronous credit exposure service that may throttle responses.
 */
public class CreditExposureService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreditExposureService.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final MockDatabase database;

    public CreditExposureService(MockDatabase database) {
        this.database = database;
    }

    public CompletableFuture<Boolean> validateExposureAsync(String traderName, BigDecimal projectedExposure) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Trader> traderOpt = database.findTrader(traderName);
            if (traderOpt.isEmpty()) {
                throw new IllegalArgumentException("Trader not registered: " + traderName);
            }
            Trader trader = traderOpt.get();
            simulateLatency();
            if (projectedExposure.compareTo(trader.getCreditLimit()) > 0) {
                LOGGER.warn("Trader {} breach credit limit. projected={}, limit={}", traderName, projectedExposure, trader.getCreditLimit());
                return false;
            }
            LOGGER.debug("Trader {} exposure validated at {}", traderName, projectedExposure);
            return true;
        }, executor);
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private void simulateLatency() {
        try {
            long delay = (long) (Math.random() * Duration.ofMillis(400).toMillis());
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

