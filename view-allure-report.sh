#!/bin/bash

# Script to view Allure report properly via web server
# This is required because Allure reports need to be served via HTTP, not file://

cd "$(dirname "$0")"

if [ ! -d "target/allure-report" ]; then
    echo "❌ Allure report not found. Please run: mvn clean verify allure:report"
    exit 1
fi

PORT=8080

# Check if port is already in use
if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo "Port $PORT is already in use. Using port 8081 instead."
    PORT=8081
fi

echo "🚀 Starting Allure report server on http://localhost:$PORT"
echo "📊 Open this URL in your browser: http://localhost:$PORT"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

# Start Python HTTP server
python3 -m http.server $PORT --directory target/allure-report

