package com.trading.robot.util;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Simple retry policy with exponential backoff.
 */
public class RetryPolicy {

    private final int maxAttempts;
    private final Duration initialBackoff;
    private final double multiplier;

    public RetryPolicy(int maxAttempts, Duration initialBackoff, double multiplier) {
        this.maxAttempts = maxAttempts;
        this.initialBackoff = initialBackoff;
        this.multiplier = multiplier;
    }

    public <T> T execute(Supplier<T> supplier) {
        Duration currentBackoff = initialBackoff;
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return supplier.get();
            } catch (RuntimeException ex) {
                lastException = ex;
                if (attempt == maxAttempts) {
                    break;
                }
                sleep(currentBackoff);
                currentBackoff = multiply(currentBackoff);
            }
        }
        throw lastException == null
            ? new IllegalStateException("Retry policy failed without exception context")
            : lastException;
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry backoff interrupted", e);
        }
    }

    private Duration multiply(Duration duration) {
        long millis = (long) (duration.toMillis() * multiplier);
        return Duration.ofMillis(Math.max(millis, duration.toMillis()));
    }
}

