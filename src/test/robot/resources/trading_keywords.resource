*** Settings ***
Library           com.trading.robot.keywords.TradingKeywords
Library           Collections
Library           BuiltIn
Library           OperatingSystem

*** Variables ***
${DATA_CSV}       ${CURDIR}/../../resources/data/trades.csv
${TOLERANCE_BPS}  25
${MAX_RETRIES}    3

*** Keywords ***
Initialize Market
    @{isins}=    Create List    US1234567890    GB0987654321    DE4455667788    FR5566778899    JP6677889900
    Start Market Data Feed    ${isins}
    FOR    ${isin}    IN    @{isins}
        Subscribe To Market Feed    ${isin}
    END

Create Trade From Data
    [Arguments]    ${row}
    ${trade_id}=   Create Trade    ${row['isin']}    ${row['trader']}    ${row['quantity']}    ${row['side']}    ${row['limitPrice']}
    Return From Keyword    ${trade_id}

Execute Trade And Confirm
    [Arguments]    ${trade_id}    ${tolerance_bps}=${TOLERANCE_BPS}
    Execute Trade    ${trade_id}
    Confirm Trade    ${trade_id}    ${tolerance_bps}

Retry Execution With Defaults
    [Arguments]    ${trade_id}
    Retry Execution With Backoff    ${trade_id}    ${MAX_RETRIES}

