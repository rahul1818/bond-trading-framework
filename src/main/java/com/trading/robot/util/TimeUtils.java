package com.trading.robot.util;

import java.time.Duration;
import java.time.Instant;

/**
 * Time helper utilities.
 */
public final class TimeUtils {

    private TimeUtils() {
    }

    public static long betweenMillis(Instant start, Instant end) {
        return Duration.between(start, end).toMillis();
    }
}

