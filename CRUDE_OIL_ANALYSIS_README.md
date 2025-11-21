# Crude Oil Technical Analysis System

## Overview

This implementation provides a comprehensive crude oil technical analysis system with MySQL database schema, REST API endpoints, and advanced volume-based indicators alongside traditional price indicators.

## Architecture

### Database Schema

#### Table 1: `crude_ohlcv_data`
Stores raw OHLCV (Open, High, Low, Close, Volume) candle data.

**Key Features:**
- Supports multiple timeframes: 4H, 1H, 15M
- Unique constraint on (symbol, timeframe, timestamp)
- Optimized indexes for query performance
- Audit fields (created_at, updated_at)

#### Table 2: `crude_technical_indicators`
Stores calculated technical indicators with foreign key relationship to OHLCV data.

**Includes:**
- **Price Indicators:** RSI, MACD, Bollinger Bands, ATR, SMAs (20/50/100/200), EMAs (12/26)
- **Volume Indicators:** OBV, OBV EMA, VWAP, Volume SMA, Volume Ratio, PVT, Volume ROC
- **Support/Resistance:** Highest/Lowest 20, Dynamic support/resistance levels
- **Metadata:** Data quality flag, Day type (Regular/EIA_Release/Fed_Event/etc.)

## Volume Indicators Explained

### 1. On-Balance Volume (OBV)
**Purpose:** Measures cumulative buying/selling pressure
**Formula:** 
- If close > prev_close: OBV = prev_OBV + volume
- If close < prev_close: OBV = prev_OBV - volume
- If close = prev_close: OBV = prev_OBV

**Interpretation:**
- ✅ Rising OBV + Rising Price = Strong uptrend confirmation
- ⚠️ Falling OBV + Rising Price = Weak trend, possible reversal
- ❌ Falling OBV + Falling Price = Strong downtrend confirmation

### 2. OBV EMA (10-period)
**Purpose:** Smooths OBV for clearer trend signals
**Formula:** Exponential Moving Average of OBV values

### 3. VWAP (Volume Weighted Average Price)
**Purpose:** Shows average price weighted by volume
**Formula:** Σ(Typical Price × Volume) / Σ(Volume)
- Typical Price = (High + Low + Close) / 3
- Resets daily for intraday timeframes

**Interpretation:**
- Price > VWAP = Bullish bias (buyers in control)
- Price < VWAP = Bearish bias (sellers in control)
- Acts as dynamic support/resistance

### 4. Volume SMA & Volume Ratio
**Purpose:** Compares current volume to average
**Formula:** Volume Ratio = (Current Volume / Volume_SMA_20) × 100

**Interpretation:**
- 150%+ = Very high volume (strong conviction)
- 100-150% = Above average (moderate conviction)
- 50-100% = Below average (weak conviction)
- <50% = Very low volume (avoid trading)

### 5. Price Volume Trend (PVT)
**Purpose:** Volume-weighted momentum indicator
**Formula:** PVT = prev_PVT + [(close - prev_close) / prev_close] × volume

**Interpretation:**
- Rising PVT = Accumulation (buying pressure)
- Falling PVT = Distribution (selling pressure)
- Divergence with price = Potential reversal signal

### 6. Volume Rate of Change
**Purpose:** Measures volume momentum
**Formula:** [(current_volume - prev_volume) / prev_volume] × 100

## API Endpoints

### Base URL
```
http://localhost:8080/api/v1/crude
```

### 1. Calculate Indicators
**Endpoint:** `POST /api/v1/crude/calculate-indicators`

**Request Body:**
```json
{
  "symbol": "BRN",
  "timeframes": ["4H", "1H", "15M"],
  "startDate": "2025-10-31T00:00:00",
  "endDate": "2025-11-20T23:59:59",
  "recalculate": false
}
```

**Response:**
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

### 2. Download Consolidated Data
**Endpoint:** `GET /api/v1/crude/download-indicators`

**Query Parameters:**
- `timeframes` (default: "4H,1H,15M")
- `startDate` (optional, ISO format)
- `endDate` (optional, ISO format)
- `dataQuality` (optional: "Fresh,Recent,Stale")
- `format` (default: "csv", options: "csv", "json")

**Example:**
```
GET /api/v1/crude/download-indicators?timeframes=4H,1H&format=csv
```

**CSV Output Columns:**
timestamp, timeframe, open, high, low, close, volume, rsi_14, macd_line, macd_signal, macd_histogram, macd_crossover_signal, bb_upper, bb_middle, bb_lower, bb_width, atr_14, sma_20, sma_50, sma_100, sma_200, price_vs_sma20, price_vs_sma50, price_vs_sma100, price_vs_sma200, highest_20, lowest_20, support_1, support_2, resistance_1, resistance_2, obv, obv_ema, vwap, volume_sma_20, volume_ratio, price_volume_trend, volume_rate_of_change, data_quality_flag, day_type

### 3. Get Latest Indicators
**Endpoint:** `GET /api/v1/crude/indicators/latest`

**Query Parameters:**
- `timeframes` (default: "4H,1H,15M")
- `limit` (default: 20)

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
    ]
  }
}
```

### Health Check
**Endpoint:** `GET /api/v1/crude/health`

### API Info
**Endpoint:** `GET /api/v1/crude/api-info`

## Configuration

Located in `application.yml`:

```yaml
crude:
  indicators:
    min-candles: 200        # Minimum candles required for calculation
    batch-size: 100         # Batch size for processing
  vwap:
    reset-daily: true       # Reset VWAP at start of each trading day
  obv:
    ema-period: 10          # OBV EMA smoothing period
  volume:
    sma-period: 20          # Volume SMA period
  export:
    csv-delimiter: ","
    date-format: "yyyy-MM-dd'T'HH:mm:ss'Z'"
    max-rows: 10000
  api:
    rate-limit: 100         # API requests per minute
```

## Key Components

### Entities
1. **CrudeOHLCVData** - Raw candle data
2. **CrudeTechnicalIndicator** - Calculated indicators

### Repositories
1. **CrudeOHLCVDataRepository** - OHLCV data access
2. **CrudeTechnicalIndicatorRepository** - Indicator data access

### Services
1. **VolumeIndicatorService** - Volume indicator calculations
2. **CrudeOilIndicatorService** - Main service orchestrating calculations
3. **CommodityAnalysisService** - Leverages existing `getCrudeOilIndicators` method

### DTOs
1. **CrudeIndicatorCalculationRequest/Response** - API request/response
2. **ConsolidatedCrudeIndicatorDTO** - Export format
3. **LatestCrudeIndicatorDTO** - Real-time summary

## Integration with Existing Code

This implementation **leverages** your existing `getCrudeOilIndicators` method from `CommodityAnalysisService`:

```java
public List<TechnicalIndicatorDTO> getCrudeOilIndicators(String interval, LocalDate date) {
    // Existing implementation that fetches data and calculates indicators
}
```

The new system extends this by:
1. Adding dedicated tables for crude oil data
2. Implementing volume-based indicators
3. Providing REST API endpoints
4. Supporting multiple timeframes (4H, 1H, 15M)
5. Enabling CSV/JSON export

## Database Migrations

Two Flyway migrations are included:
- `V12__create_crude_ohlcv_data_table.sql`
- `V13__create_crude_technical_indicators_table.sql`

These will be automatically applied on application startup.

## Usage Example

### 1. Store OHLCV Data
First, populate the `crude_ohlcv_data` table with historical data from your existing data source (FivePaisa service).

### 2. Calculate Indicators
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

### 3. Download Data
```bash
curl "http://localhost:8080/api/v1/crude/download-indicators?timeframes=4H,1H,15M&format=csv" \
  -o crude_indicators.csv
```

### 4. Get Latest Data
```bash
curl "http://localhost:8080/api/v1/crude/indicators/latest?timeframes=4H&limit=10"
```

## Data Quality Flags

- **Fresh:** Data within last 1 hour
- **Recent:** Data within last 24 hours
- **Stale:** Data older than 24 hours

## Day Type Classification

Can be enhanced with event calendar integration:
- **Regular:** Normal trading day
- **EIA_Release:** EIA inventory report day
- **Fed_Event:** Federal Reserve announcement
- **OPEC_Update:** OPEC meeting/announcement
- **Geopolitical:** Significant geopolitical event

## Performance Considerations

1. **Batch Processing:** Indicators are calculated in batches (configurable)
2. **Indexed Queries:** All queries use indexed columns
3. **Connection Pooling:** HikariCP configuration optimized
4. **Data Retention:** Consider archiving old data (add scheduled cleanup)

## Future Enhancements

1. **Real-time Updates:** WebSocket support for live indicator updates
2. **Event Calendar:** Integration with economic calendar API
3. **Machine Learning:** Pattern recognition using historical indicators
4. **Alerts:** Configurable alerts based on indicator thresholds
5. **Multi-Commodity:** Extend to support other commodities (Gold, Silver, etc.)

## Technical Stack

- **Framework:** Spring Boot 3.x
- **Database:** MySQL 8.x
- **ORM:** Hibernate/JPA
- **Migration:** Flyway
- **Technical Analysis:** TA-Lib
- **Export:** OpenCSV, Jackson

## Testing

Example test cases to implement:
1. Calculate indicators with sufficient data
2. Handle insufficient data gracefully
3. Validate VWAP calculation with daily reset
4. Test OBV calculation accuracy
5. Verify CSV export format
6. Test API rate limiting

## Monitoring

Use Spring Boot Actuator endpoints:
- `/actuator/health` - Application health
- `/actuator/metrics` - Performance metrics
- `/actuator/prometheus` - Prometheus metrics export

## Support

For issues or questions:
1. Check logs in `logs/stock-analyzer.log`
2. Verify database connectivity
3. Ensure Flyway migrations completed successfully
4. Check API endpoints with `/api/v1/crude/health`

---

**Version:** 1.0  
**Last Updated:** November 20, 2025  
**Maintained by:** Stock Analyzer Team

