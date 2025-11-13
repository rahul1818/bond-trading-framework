package com.trading.robot.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a trader with credit thresholds.
 */
public class Trader {

    private final String name;
    private final BigDecimal creditLimit;
    private final BigDecimal tradeLimit;
    private final AtomicReference<BigDecimal> currentExposure = new AtomicReference<>(BigDecimal.ZERO);

    public Trader(String name, BigDecimal creditLimit, BigDecimal tradeLimit) {
        this.name = name;
        this.creditLimit = creditLimit;
        this.tradeLimit = tradeLimit;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public BigDecimal getTradeLimit() {
        return tradeLimit;
    }

    public BigDecimal getCurrentExposure() {
        return currentExposure.get();
    }

    public void setCurrentExposure(BigDecimal exposure) {
        currentExposure.set(exposure);
    }

    public BigDecimal addExposure(BigDecimal delta) {
        return currentExposure.updateAndGet(current -> current.add(delta));
    }

    public BigDecimal reduceExposure(BigDecimal delta) {
        return currentExposure.updateAndGet(current -> current.subtract(delta));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Trader trader)) {
            return false;
        }
        return Objects.equals(name, trader.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Trader{" +
            "name='" + name + '\'' +
            ", creditLimit=" + creditLimit +
            ", tradeLimit=" + tradeLimit +
            ", currentExposure=" + currentExposure +
            '}';
    }
}

