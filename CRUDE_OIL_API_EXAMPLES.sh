#!/bin/bash

# Crude Oil Technical Analysis API - Example Usage
# This script demonstrates all API endpoints with example requests

BASE_URL="http://localhost:8080/api/v1/crude"

echo "=================================="
echo "Crude Oil Analysis API Examples"
echo "=================================="
echo ""

# 1. Health Check
echo "1. Health Check"
echo "   Endpoint: GET /health"
curl -X GET "${BASE_URL}/health"
echo -e "\n\n"

# 2. API Information
echo "2. API Information"
echo "   Endpoint: GET /api-info"
curl -X GET "${BASE_URL}/api-info"
echo -e "\n\n"

# 3. Load OHLCV Data
echo "3. Load OHLCV Data (from FivePaisa)"
echo "   Endpoint: POST /load-ohlcv-data"
curl -X POST "${BASE_URL}/load-ohlcv-data?symbol=BRN&startDate=2025-10-01&endDate=2025-11-20"
echo -e "\n\n"

# 4. Check OHLCV Statistics
echo "4. OHLCV Data Statistics"
echo "   Endpoint: GET /ohlcv-stats"
curl -X GET "${BASE_URL}/ohlcv-stats?symbol=BRN"
echo -e "\n\n"

# 5. Calculate Indicators - All Timeframes
echo "5. Calculate Indicators (All Timeframes)"
echo "   Endpoint: POST /calculate-indicators"
curl -X POST "${BASE_URL}/calculate-indicators" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "BRN",
    "timeframes": ["4H", "1H", "15M"],
    "startDate": "2025-10-31T00:00:00",
    "endDate": "2025-11-20T23:59:59",
    "recalculate": false
  }'
echo -e "\n\n"

# 6. Calculate Indicators - Single Timeframe
echo "6. Calculate Indicators (1H only)"
echo "   Endpoint: POST /calculate-indicators"
curl -X POST "${BASE_URL}/calculate-indicators" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "BRN",
    "timeframes": ["1H"],
    "recalculate": false
  }'
echo -e "\n\n"

# 7. Calculate Indicators - Force Recalculation
echo "7. Calculate Indicators (Force Recalculate)"
echo "   Endpoint: POST /calculate-indicators"
curl -X POST "${BASE_URL}/calculate-indicators" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "BRN",
    "timeframes": ["4H"],
    "recalculate": true
  }'
echo -e "\n\n"

# 8. Download Indicators - CSV (All Timeframes)
echo "8. Download Indicators - CSV (All Timeframes)"
echo "   Endpoint: GET /download-indicators"
curl -X GET "${BASE_URL}/download-indicators?timeframes=4H,1H,15M&format=csv" \
  -o crude_indicators_all.csv
echo "   Saved to: crude_indicators_all.csv"
echo -e "\n"

# 9. Download Indicators - CSV (Single Timeframe)
echo "9. Download Indicators - CSV (1H only)"
echo "   Endpoint: GET /download-indicators"
curl -X GET "${BASE_URL}/download-indicators?timeframes=1H&format=csv" \
  -o crude_indicators_1h.csv
echo "   Saved to: crude_indicators_1h.csv"
echo -e "\n"

# 10. Download Indicators - JSON
echo "10. Download Indicators - JSON"
echo "    Endpoint: GET /download-indicators"
curl -X GET "${BASE_URL}/download-indicators?timeframes=4H&format=json" \
  -o crude_indicators_4h.json
echo "    Saved to: crude_indicators_4h.json"
echo -e "\n"

# 11. Download with Date Range Filter
echo "11. Download with Date Range"
echo "    Endpoint: GET /download-indicators"
curl -X GET "${BASE_URL}/download-indicators?timeframes=1H&startDate=2025-11-15T00:00:00&endDate=2025-11-20T23:59:59&format=csv" \
  -o crude_indicators_nov15_20.csv
echo "    Saved to: crude_indicators_nov15_20.csv"
echo -e "\n"

# 12. Download with Data Quality Filter
echo "12. Download with Data Quality Filter (Fresh & Recent only)"
echo "    Endpoint: GET /download-indicators"
curl -X GET "${BASE_URL}/download-indicators?timeframes=4H&dataQuality=Fresh,Recent&format=csv" \
  -o crude_indicators_fresh.csv
echo "    Saved to: crude_indicators_fresh.csv"
echo -e "\n"

# 13. Get Latest Indicators - All Timeframes
echo "13. Get Latest Indicators (All Timeframes)"
echo "    Endpoint: GET /indicators/latest"
curl -X GET "${BASE_URL}/indicators/latest?timeframes=4H,1H,15M&limit=5"
echo -e "\n\n"

# 14. Get Latest Indicators - Single Timeframe
echo "14. Get Latest Indicators (1H only)"
echo "    Endpoint: GET /indicators/latest"
curl -X GET "${BASE_URL}/indicators/latest?timeframes=1H&limit=10"
echo -e "\n\n"

# 15. Get Latest Indicators - Limit 1 (Most Recent)
echo "15. Get Latest Indicators (Most Recent Only)"
echo "    Endpoint: GET /indicators/latest"
curl -X GET "${BASE_URL}/indicators/latest?timeframes=4H&limit=1"
echo -e "\n\n"

echo "=================================="
echo "All API Examples Completed!"
echo "=================================="

# Additional Examples with jq (JSON processor)
# Uncomment if jq is installed

# echo ""
# echo "=================================="
# echo "Pretty Print Examples (using jq)"
# echo "=================================="
# echo ""

# echo "Latest Indicators (Pretty Print)"
# curl -s -X GET "${BASE_URL}/indicators/latest?timeframes=1H&limit=3" | jq '.'
# echo -e "\n"

# echo "Health Check (Pretty Print)"
# curl -s -X GET "${BASE_URL}/health" | jq '.'
# echo -e "\n"

# echo "OHLCV Stats (Pretty Print)"
# curl -s -X GET "${BASE_URL}/ohlcv-stats?symbol=BRN" | jq '.'
# echo -e "\n"

