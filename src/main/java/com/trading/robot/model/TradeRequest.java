package com.trading.robot.model;

import java.math.BigDecimal;

/**
 * Represents an incoming trade creation request.
 */
public class TradeRequest {

    private final String isin;
    private final String traderName;
    private final int quantity;
    private final TradeSide side;
    private final BigDecimal limitPrice;

    public TradeRequest(String isin, String traderName, int quantity, TradeSide side, BigDecimal limitPrice) {
        this.isin = isin;
        this.traderName = traderName;
        this.quantity = quantity;
        this.side = side;
        this.limitPrice = limitPrice;
    }

    public String getIsin() {
        return isin;
    }

    public String getTraderName() {
        return traderName;
    }

    public int getQuantity() {
        return quantity;
    }

    public TradeSide getSide() {
        return side;
    }

    public BigDecimal getLimitPrice() {
        return limitPrice;
    }
}

