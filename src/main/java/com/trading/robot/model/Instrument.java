package com.trading.robot.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents a bond instrument with pricing metadata.
 */
public class Instrument {

    private final String isin;
    private final String name;
    private final String currency;
    private final BigDecimal yield;
    private final int tenorYears;

    public Instrument(String isin, String name, String currency, BigDecimal yield, int tenorYears) {
        this.isin = isin;
        this.name = name;
        this.currency = currency;
        this.yield = yield;
        this.tenorYears = tenorYears;
    }

    public String getIsin() {
        return isin;
    }

    public String getName() {
        return name;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getYield() {
        return yield;
    }

    public int getTenorYears() {
        return tenorYears;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Instrument instrument)) {
            return false;
        }
        return Objects.equals(isin, instrument.isin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isin);
    }

    @Override
    public String toString() {
        return "Instrument{" +
            "isin='" + isin + '\'' +
            ", name='" + name + '\'' +
            ", currency='" + currency + '\'' +
            ", yield=" + yield +
            ", tenorYears=" + tenorYears +
            '}';
    }
}

