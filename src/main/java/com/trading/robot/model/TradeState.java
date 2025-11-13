package com.trading.robot.model;

/**
 * Represents the lifecycle states a trade can traverse.
 */
public enum TradeState {
    CREATED,
    EXECUTED,
    PARTIALLY_FILLED,
    PENDING_CONFIRMATION,
    CONFIRMED,
    REJECTED,
    CANCELLED,
    RETRY
}

