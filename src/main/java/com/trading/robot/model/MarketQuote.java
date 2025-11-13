package com.trading.robot.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.math.RoundingMode;

/**
 * Represents a single market data tick.
 */
public class MarketQuote {

    private final String isin;
    private final BigDecimal bid;
    private final BigDecimal ask;
    private final BigDecimal mid;
    private final Instant timestamp;

    public MarketQuote(String isin, BigDecimal bid, BigDecimal ask, Instant timestamp) {
        this.isin = isin;
        this.bid = bid;
        this.ask = ask;
        this.mid = bid.add(ask).divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
        this.timestamp = timestamp;
    }

    public String getIsin() {
        return isin;
    }

    public BigDecimal getBid() {
        return bid;
    }

    public BigDecimal getAsk() {
        return ask;
    }

    public BigDecimal getMid() {
        return mid;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}

