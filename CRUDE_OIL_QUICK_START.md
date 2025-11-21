# Crude Oil Analysis - Quick Start Guide

## Step-by-Step Setup

### 1. Database Setup

The database tables will be automatically created via Flyway migrations on application startup.

**Migrations:**
- `V12__create_crude_ohlcv_data_table.sql`
- `V13__create_crude_technical_indicators_table.sql`

### Important: 5paisa API Interval Mapping

**5paisa supports the following intervals:**
- `15m` - 15 minutes ✅
- `60m` - 60 minutes (1 hour) ✅
- **Note:** 5paisa does NOT support `1h` or `4h` directly

**Our System Timeframe Mapping:**
- `15M` (15 minutes) → Uses `15m` from 5paisa
- `1H` (1 hour) → Uses `60m` from 5paisa
- `4H` (4 hours) → **Computed by aggregating `60m` candles**

The system automatically:
1. Fetches `60m` data from 5paisa
2. Aggregates four 60-minute candles into one 4-hour candle
3. Maintains proper OHLCV values (Open from first, Close from last, High/Low from all, Volume summed)

### 2. Load OHLCV Data

You can load OHLCV data in two ways:

#### Option A: Use the Data Loader Service (Programmatic)

```java
@Autowired
private CrudeOHLCVDataLoaderService loaderService;

// Load data for all timeframes
LocalDate startDate = LocalDate.now().minusDays(60);
LocalDate endDate = LocalDate.now();
int recordsLoaded = loaderService.loadAllTimeframes("BRN", startDate, endDate);
```

#### Option B: Insert Data Manually

```sql
INSERT INTO crude_ohlcv_data (timestamp, timeframe, symbol, open, high, low, close, volume)
VALUES 
  ('2025-11-20 08:00:00', '1H', 'BRN', 82.50, 83.00, 82.30, 82.80, 15000),
  ('2025-11-20 09:00:00', '1H', 'BRN', 82.80, 83.20, 82.70, 83.10, 18000);
```

### 3. Calculate Indicators

#### Using REST API

```bash
curl -X POST http://localhost:8080/api/v1/crude/calculate-indicators \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "BRN",
    "timeframes": ["4H", "1H", "15M"],
    "startDate": "2025-10-31T00:00:00",
    "endDate": "2025-11-20T23:59:59",
    "recalculate": false
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Indicators calculated successfully",
  "data": {
    "processed": {
      "4H": 200,
      "1H": 200,
      "15M": 200
    },
    "totalRecords": 600,
    "executionTimeMs": 1850
  }
}
```

### 4. Download Results

#### As CSV
```bash
curl "http://localhost:8080/api/v1/crude/download-indicators?timeframes=4H,1H,15M&format=csv" \
  -o crude_indicators.csv
```

#### As JSON
```bash
curl "http://localhost:8080/api/v1/crude/download-indicators?timeframes=4H&format=json" \
  -o crude_indicators.json
```

#### With Date Filters
```bash
curl "http://localhost:8080/api/v1/crude/download-indicators?timeframes=1H&startDate=2025-11-01T00:00:00&endDate=2025-11-20T23:59:59&format=csv" \
  -o crude_indicators_november.csv
```

#### Filter by Data Quality
```bash
curl "http://localhost:8080/api/v1/crude/download-indicators?timeframes=4H&dataQuality=Fresh,Recent&format=csv" \
  -o crude_indicators_fresh.csv
```

### 5. Query Latest Indicators

```bash
curl "http://localhost:8080/api/v1/crude/indicators/latest?timeframes=4H,1H,15M&limit=20"
```

**Response:**
```json
{
  "success": true,
  "data": {
    "4H": [
      {
        "timestamp": "2025-11-20T20:00:00Z",
        "close": 81.95,
        "rsi14": 58.34,
        "macdCrossoverSignal": "bullish",
        "obv": 45673210,
        "vwap": 81.50,
        "volumeRatio": 115.23,
        "dataQualityFlag": "Fresh"
      }
    ],
    "1H": [...],
    "15M": [...]
  }
}
```

## Common Use Cases

### Use Case 1: Daily Analysis Workflow

```bash
# 1. Load today's data (would be automated in production)
# This would be done programmatically or via scheduled job

# 2. Calculate indicators
curl -X POST http://localhost:8080/api/v1/crude/calculate-indicators \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "BRN",
    "timeframes": ["1H"],
    "recalculate": false
  }'

# 3. Get latest analysis
curl "http://localhost:8080/api/v1/crude/indicators/latest?timeframes=1H&limit=5"

# 4. Download full report
curl "http://localhost:8080/api/v1/crude/download-indicators?timeframes=1H&format=csv" \
  -o daily_analysis.csv
```

### Use Case 2: Backtest Historical Data

```bash
# Calculate indicators for last 3 months
curl -X POST http://localhost:8080/api/v1/crude/calculate-indicators \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "BRN",
    "timeframes": ["4H", "1H", "15M"],
    "startDate": "2025-08-20T00:00:00",
    "endDate": "2025-11-20T23:59:59",
    "recalculate": true
  }'

# Download results for analysis
curl "http://localhost:8080/api/v1/crude/download-indicators?timeframes=4H,1H,15M&startDate=2025-08-20T00:00:00&endDate=2025-11-20T23:59:59&format=csv" \
  -o backtest_3months.csv
```

### Use Case 3: Real-Time Monitoring

```bash
# Check health
curl http://localhost:8080/api/v1/crude/health

# Get latest indicators every 5 minutes (in a loop or scheduled job)
while true; do
  curl "http://localhost:8080/api/v1/crude/indicators/latest?timeframes=15M&limit=1"
  sleep 300  # Wait 5 minutes
done
```

## Integration with Existing System

### Leveraging getCrudeOilIndicators

The new system is designed to work alongside your existing `getCrudeOilIndicators` method:

```java
// Existing method - still works
List<TechnicalIndicatorDTO> indicators = commodityAnalysisService
    .getCrudeOilIndicators("15m", LocalDate.now());

// New system - enhanced with volume indicators
CrudeIndicatorCalculationRequest request = CrudeIndicatorCalculationRequest.builder()
    .symbol("BRN")
    .timeframes(Arrays.asList("15M"))
    .build();
CrudeIndicatorCalculationResponse response = crudeOilIndicatorService
    .calculateIndicators(request);
```

## SQL Queries for Analysis

### Get Latest Indicators with OHLCV Data
```sql
SELECT 
    o.timestamp,
    o.timeframe,
    o.close,
    i.rsi_14,
    i.macd_crossover_signal,
    i.obv,
    i.vwap,
    i.volume_ratio,
    i.price_volume_trend,
    i.data_quality_flag
FROM crude_ohlcv_data o
JOIN crude_technical_indicators i ON o.id = i.ohlcv_id
WHERE o.timeframe = '1H'
  AND o.timestamp >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
ORDER BY o.timestamp DESC;
```

### Find Strong Bullish Signals
```sql
SELECT 
    o.timestamp,
    o.close,
    i.rsi_14,
    i.macd_crossover_signal,
    i.volume_ratio,
    i.data_quality_flag
FROM crude_ohlcv_data o
JOIN crude_technical_indicators i ON o.id = i.ohlcv_id
WHERE o.timeframe = '4H'
  AND i.rsi_14 > 50
  AND i.macd_crossover_signal = 'bullish'
  AND i.volume_ratio > 120
  AND i.data_quality_flag = 'Fresh'
ORDER BY o.timestamp DESC
LIMIT 10;
```

### Volume Analysis
```sql
SELECT 
    o.timestamp,
    o.timeframe,
    o.volume,
    i.volume_sma_20,
    i.volume_ratio,
    i.obv,
    i.price_volume_trend
FROM crude_ohlcv_data o
JOIN crude_technical_indicators i ON o.id = i.ohlcv_id
WHERE o.timeframe = '1H'
  AND i.volume_ratio > 150  -- High volume
ORDER BY o.timestamp DESC
LIMIT 20;
```

## Troubleshooting

### Issue: No data returned from API

**Check 1:** Verify OHLCV data exists
```sql
SELECT COUNT(*) FROM crude_ohlcv_data WHERE timeframe = '1H';
```

**Check 2:** Verify indicators are calculated
```sql
SELECT COUNT(*) FROM crude_technical_indicators WHERE timeframe = '1H';
```

**Check 3:** Check application logs
```bash
tail -f logs/stock-analyzer.log | grep -i crude
```

### Issue: Insufficient data error

**Solution:** Ensure you have at least 200 candles loaded
```sql
SELECT timeframe, COUNT(*) as count 
FROM crude_ohlcv_data 
GROUP BY timeframe;
```

If counts are low, load more historical data:
```java
loaderService.loadAllTimeframes("BRN", 
    LocalDate.now().minusDays(90), 
    LocalDate.now());
```

### Issue: Slow calculation performance

**Solution 1:** Increase batch size in configuration
```yaml
crude:
  indicators:
    batch-size: 200  # Increase from default 100
```

**Solution 2:** Add database indexes (already included in migrations)

**Solution 3:** Use recalculate=false to skip existing indicators

## Performance Benchmarks

Expected performance on moderate hardware:
- **OHLCV Loading:** ~500 records/second
- **Indicator Calculation:** ~200 candles/second
- **CSV Export:** ~1000 records/second
- **JSON Export:** ~800 records/second

## Next Steps

1. **Set up scheduled jobs** to automatically load OHLCV data
2. **Implement alerts** based on indicator thresholds
3. **Create dashboards** using the exported data
4. **Add event calendar integration** for day_type classification
5. **Implement WebSocket** for real-time updates

## Support Resources

- API Documentation: `GET /api/v1/crude/api-info`
- Health Check: `GET /api/v1/crude/health`
- Full Documentation: See `CRUDE_OIL_ANALYSIS_README.md`

---

**Questions?** Check the logs or API documentation for detailed error messages.

