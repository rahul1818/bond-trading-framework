package com.trading.robot.model;

import java.time.Instant;

/**
 * Captures a trade state transition with metadata.
 */
public class TradeStateChange {

    private final TradeState state;
    private final Instant timestamp;
    private final String reason;

    public TradeStateChange(TradeState state, Instant timestamp, String reason) {
        this.state = state;
        this.timestamp = timestamp;
        this.reason = reason;
    }

    public TradeState getState() {
        return state;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getReason() {
        return reason;
    }
}

