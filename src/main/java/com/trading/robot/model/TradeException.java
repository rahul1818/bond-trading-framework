package com.trading.robot.model;

/**
 * Custom exception thrown for trade lifecycle issues.
 */
public class TradeException extends RuntimeException {

    public TradeException(String message) {
        super(message);
    }

    public TradeException(String message, Throwable cause) {
        super(message, cause);
    }
}

