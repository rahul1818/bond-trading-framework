# Bond Trading Robot Framework

A comprehensive end-to-end test automation framework for bond trading lifecycle management. This framework simulates a complete trading ecosystem with real-time market data, trade execution, risk validation, and resiliency features using **Robot Framework** with a **Java backend**.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Project Structure](#project-structure)
- [Running Tests](#running-tests)
- [Viewing Results](#viewing-results)
- [Test Scenarios](#test-scenarios)
- [Architecture](#architecture)
- [Custom Keywords](#custom-keywords)
- [CI/CD Integration](#cicd-integration)

## 🎯 Overview

This framework automates the complete bond trading lifecycle from trade creation through execution, confirmation, and risk validation. It includes:

- **Mock Services**: Instrument, Market Data, Trade Execution, Credit Exposure
- **Asynchronous Operations**: Real-time market data feeds with Java threads
- **Resiliency Features**: Retry logic, network failure simulation, partial fills
- **Risk Management**: Trade limits, credit exposure validation
- **Comprehensive Reporting**: Allure reports with detailed test results

## ✨ Features

- ✅ Multi-user concurrent trading simulation
- ✅ Real-time market data with configurable update intervals
- ✅ Trade lifecycle management (CREATED → EXECUTED → CONFIRMED/REJECTED)
- ✅ Automatic retry with exponential backoff
- ✅ Network failure and latency simulation
- ✅ Risk limit and credit exposure validation
- ✅ Data-driven testing via CSV
- ✅ Allure reporting integration
- ✅ CI/CD ready (Jenkins, GitHub Actions)

## 📦 Prerequisites

Before running the framework, ensure you have:

- **Java 17+** (JDK)
- **Maven 3.9+**
- **Python 3.10+** with `pip`
- **Allure Commandline** (optional, for viewing reports)

### Verify Prerequisites

```bash
java -version    # Should show Java 17 or higher
mvn -version     # Should show Maven 3.9 or higher
python3 --version # Should show Python 3.10 or higher
```

## 🚀 Installation

### Step 1: Clone/Navigate to Project

```bash
cd bond-trading-framework
```

### Step 2: Install Python Dependencies

```bash
pip install robotframework allure-robotframework
```

**Note**: Python dependencies are minimal since the framework uses Java libraries directly.

### Step 3: Build Java Project

```bash
mvn clean compile
```

This will:
- Compile all Java source files
- Download Maven dependencies
- Build the project JAR

## 📁 Project Structure

```
bond-trading-framework/
├── src/
│   ├── main/
│   │   ├── java/com/trading/robot/
│   │   │   ├── config/
│   │   │   │   └── MockDatabase.java          # In-memory database
│   │   │   ├── keywords/
│   │   │   │   └── TradingKeywords.java       # Robot Framework keywords
│   │   │   ├── model/
│   │   │   │   ├── Instrument.java            # Bond instrument model
│   │   │   │   ├── MarketQuote.java           # Market quote model
│   │   │   │   ├── Trade.java                 # Trade entity
│   │   │   │   ├── TradeException.java        # Custom exceptions
│   │   │   │   ├── TradeRequest.java          # Trade request DTO
│   │   │   │   ├── TradeSide.java             # BUY/SELL enum
│   │   │   │   ├── TradeState.java            # Trade state enum
│   │   │   │   ├── TradeStateChange.java       # State transition history
│   │   │   │   ├── Trader.java                # Trader entity
│   │   │   │   └── ValidationResult.java       # Validation result
│   │   │   ├── reporting/
│   │   │   │   └── AllureReportGenerator.java # Allure report generator
│   │   │   ├── service/
│   │   │   │   ├── CreditExposureService.java # Credit validation
│   │   │   │   ├── InstrumentService.java     # Instrument management
│   │   │   │   ├── MarketDataService.java     # Market data feed
│   │   │   │   ├── MetricsService.java        # Trade metrics
│   │   │   │   ├── TradeDataLoader.java       # CSV data loader
│   │   │   │   ├── TradeService.java           # Trade lifecycle management
│   │   │   │   └── TradingEngine.java         # Main trading engine
│   │   │   └── util/
│   │   │       ├── RandomUtils.java            # Random data generation
│   │   │       ├── RetryPolicy.java            # Retry logic
│   │   │       └── TimeUtils.java              # Time utilities
│   │   └── resources/
│   │       └── logback.xml                     # Logging configuration
│   └── test/
│       ├── robot/
│       │   ├── resources/
│       │   │   └── trading_keywords.resource   # Robot resource file
│       │   └── tests/
│       │       └── bond_trading.robot         # Test suite
│       └── resources/
│           └── data/
│               └── trades.csv                 # Test data
├── pom.xml                                     # Maven configuration
└── README.md                                   # This file
```

## 🧪 Running Tests

### Run All Tests

```bash
mvn clean verify
```

This command will:
1. Clean previous build artifacts
2. Compile Java source code
3. Run Robot Framework test suite
4. Generate Allure results
5. Create test reports

### Run Tests Without Clean

```bash
mvn verify
```

### Run Only Compilation (No Tests)

```bash
mvn clean compile
```

## 📊 Viewing Results

### 1. Allure Report (Recommended)

The Allure report provides the most comprehensive view of test results.

#### Generate Allure Report

```bash
mvn allure:report
```

#### Open Allure Report

**⚠️ IMPORTANT**: Allure reports must be served via HTTP, not opened directly as a file. Use one of these methods:

**Method 1: Using the helper script (Recommended)**
```bash
./view-allure-report.sh
```
This will start a web server and open the report in your browser automatically.

**Method 2: Using Python HTTP server**
```bash
cd target/allure-report
python3 -m http.server 8080
```
Then open `http://localhost:8080` in your browser.

**Method 3: Using Allure CLI (if installed)**
```bash
allure serve target/robotframework/allure-results
```

**Why?** Opening `file://` URLs directly causes CORS issues and the report will show "Loading..." indefinitely. The report must be served via HTTP.

#### Allure Report Features

- **Overview**: Test summary with pass/fail statistics
- **Suites**: Test suite organization
- **Graphs**: Duration trends, status charts
- **Timeline**: Test execution timeline
- **Behaviors**: Test behavior grouping
- **Packages**: Package-level organization

### 2. Robot Framework HTML Report

```bash
open target/robotframework/report.html
```

Shows:
- Test execution summary
- Individual test results
- Keyword-level details
- Execution logs

### 3. Robot Framework Log

```bash
open target/robotframework/log.html
```

Detailed execution log with:
- All keyword calls
- Variable values
- Error messages
- Screenshots (if any)

### 4. Console Output

The Maven output shows a summary:

```
Tests.Bond Trading :: End-to-end bond trading lifecycle scenarios ... | PASS |
10 critical tests, 10 passed, 0 failed
10 tests total, 10 passed, 0 failed
```

## 🧩 Test Scenarios

The framework includes **10 comprehensive test scenarios**:

### 1. Create And Confirm Valid Trade
- Creates a trade from CSV data
- Executes the trade
- Confirms the trade
- Validates state history and price deviation

### 2. Invalid Instrument Should Fail
- Attempts to create trade with invalid ISIN
- Verifies proper error handling

### 3. Price Deviation Above Tolerance Rejects Trade
- Creates trade with high price deviation
- Executes trade
- Confirms trade should be rejected due to price deviation

### 4. Executing Already Confirmed Trade Raises Error
- Creates and confirms a trade
- Attempts to execute again
- Verifies error is raised

### 5. Retry Logic Handles Injected Failure
- Creates trade
- Injects execution failure
- Uses retry logic with backoff
- Verifies successful execution after retry

### 6. Partial Fill Execution Captured
- Creates trade
- Forces partial fill
- Verifies partial fill quantity is captured

### 7. Trade Cancelled Before Confirmation
- Creates and executes trade
- Cancels trade before confirmation
- Verifies cancellation state

### 8. Multiple Concurrent Traders
- Creates trades for multiple traders concurrently
- Executes trades concurrently
- Confirms all trades
- Validates trader exposure

### 9. Rapid Market Update Reflected
- Subscribes to market feed
- Waits for market updates
- Verifies updates are captured

### 10. Risk Breach Attempt Fails
- Attempts to create trade exceeding trader limit
- Verifies trade limit validation

## 🏗️ Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────┐
│              Robot Framework Test Suite                  │
│              (bond_trading.robot)                        │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│           TradingKeywords (Java Library)                │
│           (Exposes Robot Framework Keywords)             │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              TradingEngine (Orchestrator)                │
└─────┬──────────┬──────────┬──────────┬─────────────────┘
      │          │          │          │
      ▼          ▼          ▼          ▼
┌─────────┐ ┌─────────┐ ┌─────────┐ ┌──────────────┐
│Instrument│ │ Market  │ │  Trade  │ │   Credit     │
│ Service │ │  Data   │ │ Service │ │  Exposure    │
│         │ │ Service │ │         │ │   Service   │
└─────────┘ └─────────┘ └─────────┘ └──────────────┘
      │          │          │          │
      └──────────┴──────────┴──────────┘
                     │
                     ▼
            ┌─────────────────┐
            │  MockDatabase    │
            │  (In-Memory)     │
            └─────────────────┘
```

### Key Components

1. **TradingKeywords**: Java class exposing methods as Robot Framework keywords
2. **TradingEngine**: Main orchestrator coordinating all services
3. **MockDatabase**: In-memory storage for instruments, traders, and trades
4. **Services**: Asynchronous mock services for trading operations
5. **AllureReportGenerator**: Converts Robot Framework XML to Allure format

## 🔧 Custom Keywords

The framework provides the following custom Robot Framework keywords:

### Trade Management
- `Create Trade` - Create a new trade
- `Execute Trade` - Execute a trade
- `Confirm Trade` - Confirm an executed trade
- `Cancel Trade` - Cancel a trade
- `Get Trade Details` - Get trade information

### Advanced Operations
- `Retry Execution With Backoff` - Retry trade execution with exponential backoff
- `Inject Execution Failure` - Inject a failure for testing
- `Force Partial Fill` - Force a partial fill scenario
- `Execute Trades Concurrently` - Execute multiple trades concurrently
- `Confirm Trades` - Confirm multiple trades

### Market Data
- `Start Market Data Feed` - Start market data feed for ISINs
- `Subscribe To Market Feed` - Subscribe to market updates
- `Get Captured Market Updates` - Get captured market updates
- `Get Latest Quote` - Get latest market quote
- `Apply Market Shock` - Apply market shock to prices

### Validation & Assertions
- `Assert Trade State History` - Assert trade state transitions
- `Get Trade State History` - Get trade state history
- `Compare Execution Price To Market Average` - Compare prices
- `Validate Trader Exposure` - Validate trader credit exposure

### Data Loading
- `Load Trades From Csv` - Load trade data from CSV file

### Utilities
- `Get Execution Latency Millis` - Get execution latency
- `Get Retry Attempts` - Get retry attempt count
- `Shutdown Trading Engine` - Shutdown the trading engine

## 🔄 CI/CD Integration

### GitHub Actions

The project includes a GitHub Actions workflow (`.github/workflows/ci.yml`) that:

1. Sets up Java 17 and Maven
2. Installs Python dependencies
3. Runs tests with `mvn verify`
4. Generates Allure report
5. Uploads artifacts

### Jenkins

A sample `Jenkinsfile` is provided in `docs/Jenkinsfile`:

```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn verify'
            }
        }
        stage('Report') {
            steps {
                sh 'mvn allure:report'
                publishHTML([
                    reportDir: 'target/allure-report',
                    reportFiles: 'index.html',
                    reportName: 'Allure Report'
                ])
            }
        }
    }
}
```

## 📝 Configuration

### Logging

Logging is configured in `src/main/resources/logback.xml`. Adjust log levels as needed:

```xml
<logger name="com.trading.robot" level="INFO"/>
```

### Test Data

Test data is in `src/test/resources/data/trades.csv`. Format:

```csv
isin,trader,quantity,side,limitPrice
US1234567890,Alice,1000000,BUY,100.0
```

### Maven Configuration

Key Maven properties in `pom.xml`:

- Java version: 17
- Robot Framework plugin: 1.8.1
- Allure plugin: 2.12.0

## 🐛 Troubleshooting

### Tests Fail to Run

1. **Check Java version**: `java -version` should show 17+
2. **Check Maven**: `mvn -version` should show 3.9+
3. **Clean and rebuild**: `mvn clean verify`

### Allure Report Not Generated

1. Ensure tests completed: `mvn verify`
2. Check for Allure results: `ls target/robotframework/allure-results/`
3. Generate report: `mvn allure:report`

### Java Class Not Found

1. Recompile: `mvn clean compile`
2. Check classpath in `pom.xml`
3. Verify JAR is built: `ls target/*.jar`

## 📚 Additional Resources

- [Robot Framework Documentation](https://robotframework.org/)
- [Allure Framework Documentation](https://docs.qameta.io/allure/)
- [Maven Documentation](https://maven.apache.org/guides/)

## 📄 License

This project is created for demonstration purposes.

## 👥 Support

For issues or questions, please refer to the project documentation or contact the development team.

---

**Last Updated**: November 2024
**Version**: 1.0-SNAPSHOT
