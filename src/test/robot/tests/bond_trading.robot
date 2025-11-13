*** Settings ***
Documentation     End-to-end bond trading lifecycle scenarios with Java-backed keywords.
Resource          ../resources/trading_keywords.resource
Suite Setup       Initialize Market
Suite Teardown    Shutdown Trading Engine
Test Timeout      5 minutes

*** Test Cases ***
Create And Confirm Valid Trade
    ${rows}=            Load Trades From Csv    ${DATA_CSV}
    ${row}=             Get From List           ${rows}    0
    ${trade_id}=        Create Trade From Data  ${row}
    Sleep               3s
    ${status}=          Run Keyword And Return Status    Execute Trade And Confirm    ${trade_id}
    Run Keyword If      not ${status}    Run Keywords    Sleep    2s    AND    Execute Trade And Confirm    ${trade_id}
    ${history}=         Get Trade State History    ${trade_id}
    List Should Contain Value    ${history}    CONFIRMED
    ${deviation}=       Compare Execution Price To Market Average    ${trade_id}
    Should Be True      ${deviation} < 25

Invalid Instrument Should Fail
    Run Keyword And Expect Error    *Instrument not found*    Create Trade    INVALID123    Alice    1000    BUY    100.0

Price Deviation Above Tolerance Rejects Trade
    ${rows}=            Load Trades From Csv    ${DATA_CSV}
    ${row}=             Get From List           ${rows}    1
    ${trade_id}=        Create Trade From Data  ${row}
    Sleep               2s
    Execute Trade       ${trade_id}
    Apply Market Shock  ${row['isin']}    10
    Confirm Trade       ${trade_id}    1
    ${history}=         Get Trade State History    ${trade_id}
    List Should Contain Value    ${history}    REJECTED

Executing Already Confirmed Trade Raises Error
    ${rows}=            Load Trades From Csv    ${DATA_CSV}
    ${row}=             Get From List           ${rows}    2
    ${trade_id}=        Create Trade From Data  ${row}
    Sleep               2s
    Execute Trade And Confirm    ${trade_id}
    Run Keyword And Expect Error    *already terminal*    Execute Trade    ${trade_id}

Retry Logic Handles Injected Failure
    ${rows}=            Load Trades From Csv    ${DATA_CSV}
    ${row}=             Get From List           ${rows}    3
    ${trade_id}=        Create Trade From Data  ${row}
    Inject Execution Failure    ${trade_id}
    Sleep               2s
    Retry Execution With Defaults    ${trade_id}
    ${history}=         Get Trade State History    ${trade_id}
    List Should Contain Value    ${history}    RETRY

Partial Fill Execution Captured
    ${rows}=            Load Trades From Csv    ${DATA_CSV}
    ${row}=             Get From List           ${rows}    4
    ${trade_id}=        Create Trade From Data  ${row}
    ${half}=            Evaluate    int(${row['quantity']} / 2)
    Force Partial Fill  ${trade_id}    ${half}
    Sleep               2s
    Execute Trade       ${trade_id}
    ${details}=         Get Trade Details    ${trade_id}
    Should Be True      ${details['filledQuantity']} < ${details['quantity']}
    ${history}=         Get Trade State History    ${trade_id}
    List Should Contain Value    ${history}    PARTIALLY_FILLED

Trade Cancelled Before Confirmation
    ${rows}=            Load Trades From Csv    ${DATA_CSV}
    ${row}=             Get From List           ${rows}    0
    ${trade_id}=        Create Trade From Data  ${row}
    Sleep               2s
    Retry Execution With Defaults    ${trade_id}
    Cancel Trade        ${trade_id}    Trader requested cancellation
    ${history}=         Get Trade State History    ${trade_id}
    List Should Contain Value    ${history}    CANCELLED

Multiple Concurrent Traders
    ${rows}=            Load Trades From Csv    ${DATA_CSV}
    @{trade_ids}=       Create List
    :FOR    ${index}    IN RANGE    0    3
    \   ${row}=         Get From List           ${rows}    ${index}
    \   ${trade_id}=    Create Trade From Data  ${row}
    \   Append To List  ${trade_ids}    ${trade_id}
    END
    Sleep               2s
    Execute Trades Concurrently    ${trade_ids}
    Confirm Trades     ${trade_ids}    ${TOLERANCE_BPS}
    Validate Trader Exposure    Alice    1000000
    Validate Trader Exposure    Bob      500000

Rapid Market Update Reflected
    Subscribe To Market Feed    US1234567890
    Sleep               10s
    ${updates}=         Get Captured Market Updates    US1234567890
    ${count}=           Get Length    ${updates}
    Should Be True      ${count} >= 1    At least 1 market update should be captured, got ${count}

Risk Breach Attempt Fails
    Run Keyword And Expect Error    *Trade limit exceeded*    Create Trade    US1234567890    Bob    5000000    BUY    105.0

