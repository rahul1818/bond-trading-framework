# Models Reference Guide

This document provides a quick reference for all models used in the Bond Trading Robot Framework.

## Core Models (All Required)

### 1. **Instrument.java**
- **Purpose**: Represents a bond instrument
- **Key Fields**: ISIN, currency, yield, tenor
- **Used By**: InstrumentService, TradeService, TradingEngine

### 2. **Trader.java**
- **Purpose**: Represents a trader with limits and exposure
- **Key Fields**: name, tradeLimit, currentExposure
- **Used By**: TradeService, CreditExposureService, MockDatabase

### 3. **Trade.java**
- **Purpose**: Core trade entity with lifecycle management
- **Key Fields**: tradeId, instrument, trader, quantity, side, state, executionPrice
- **Used By**: All services, TradingEngine, TradingKeywords

### 4. **TradeRequest.java**
- **Purpose**: DTO for trade creation requests
- **Key Fields**: isin, traderName, quantity, side, limitPrice
- **Used By**: TradeService, TradeDataLoader, TradingKeywords

### 5. **MarketQuote.java**
- **Purpose**: Represents market price quote
- **Key Fields**: isin, bid, ask, mid, timestamp
- **Used By**: MarketDataService, TradeService, TradingKeywords

## Enums

### 6. **TradeSide.java**
- **Purpose**: Trade direction (BUY/SELL)
- **Values**: BUY, SELL
- **Used By**: Trade, TradeRequest, TradeDataLoader

### 7. **TradeState.java**
- **Purpose**: Trade lifecycle states
- **Values**: CREATED, EXECUTED, PENDING_CONFIRMATION, CONFIRMED, REJECTED, CANCELLED, PARTIALLY_FILLED, RETRY
- **Used By**: Trade, TradeService, TradingKeywords

## Supporting Models

### 8. **TradeStateChange.java**
- **Purpose**: Records state transitions with timestamps
- **Key Fields**: state, timestamp, reason
- **Used By**: Trade (for state history tracking)

### 9. **ValidationResult.java**
- **Purpose**: Result of validation operations
- **Key Fields**: isValid, message
- **Used By**: TradeService (for limit and exposure validation)

### 10. **TradeException.java**
- **Purpose**: Custom exception for trading errors
- **Used By**: All services for error handling

## Removed Models

- **TradeMetrics.java** - ❌ Removed (not used anywhere in the codebase)

## Model Relationships

```
Trader
  └── has many → Trade
       └── references → Instrument
            └── has many → MarketQuote

TradeRequest → creates → Trade
Trade → has many → TradeStateChange
Trade → uses → TradeSide, TradeState
```

## Usage Summary

| Model | Services Using It | Purpose |
|-------|------------------|---------|
| Instrument | InstrumentService, TradeService | Bond instrument data |
| Trader | TradeService, CreditExposureService | Trader information |
| Trade | All services | Core trade entity |
| TradeRequest | TradeService, TradeDataLoader | Trade creation input |
| MarketQuote | MarketDataService, TradeService | Market price data |
| TradeSide | Trade, TradeRequest | Trade direction |
| TradeState | Trade, TradeService | Trade lifecycle |
| TradeStateChange | Trade | State history tracking |
| ValidationResult | TradeService | Validation results |
| TradeException | All services | Error handling |

All models are essential and actively used in the framework.

