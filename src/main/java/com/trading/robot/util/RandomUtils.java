package com.trading.robot.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility helpers for randomised simulations.
 */
public final class RandomUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private RandomUtils() {
        // Utility
    }

    public static BigDecimal randomPrice(BigDecimal basePrice, double maxDeviationBps) {
        double randomBps = ThreadLocalRandom.current().nextDouble(-maxDeviationBps, maxDeviationBps);
        BigDecimal factor = BigDecimal.ONE.add(BigDecimal.valueOf(randomBps / 10_000d));
        return basePrice.multiply(factor).setScale(6, RoundingMode.HALF_UP);
    }

    public static boolean chance(double probability) {
        return ThreadLocalRandom.current().nextDouble() < probability;
    }

    public static int randomInt(int originInclusive, int boundExclusive) {
        return ThreadLocalRandom.current().nextInt(originInclusive, boundExclusive);
    }

    public static double secureRandomDouble() {
        return SECURE_RANDOM.nextDouble();
    }
}

