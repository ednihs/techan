# Interactive Charts Feature - Documentation

## Overview

Added interactive price and technical indicator charts to the index page (home page) for both Crude Oil and Equity analysis. This feature provides visual insights into market data with technical indicators to help make trading decisions easier.

## Features

### 1. **Crude Oil Analysis Charts**
   - **Price & Volume Chart**: Displays closing prices with VWAP overlay
   - **Technical Indicators Chart**: Shows RSI (14) and Volume Ratio
   - **Timeframe Selection**: Switch between 4H, 1H, and 15M timeframes
   - **Live Summary**: Displays latest values for Close, RSI, MACD Signal, Volume Ratio, and VWAP
   - **Color-coded Indicators**: 
     - RSI: Green (oversold <30), Yellow (neutral 30-70), Red (overbought >70)
     - MACD: Green (bullish), Yellow (neutral), Red (bearish)

### 2. **Equity Analysis Charts**
   - **Price & Volume Chart**: Displays closing prices with VWAP overlay
   - **Technical Indicators Chart**: Shows RSI (14) and Volume Ratio
   - **Dynamic Symbol Selection**: Enter any stock symbol (e.g., TCS, INFY, RELIANCE)
   - **Adjustable Time Range**: Select number of days (10-100)
   - **Live Summary**: Displays latest values for Close, RSI, MACD, Volume Ratio, and VWAP
   - **Color-coded Indicators**: Same as crude oil

## Technical Implementation

### Frontend Components

#### 1. **HTML Structure** (`index.html`)
```html
<!-- Added Chart.js library -->
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>

<!-- Market Charts Section -->
- Crude Oil Charts (2 canvas elements)
- Equity Charts (2 canvas elements)
- Control panels for each section
- Summary panels showing key metrics
```

#### 2. **CSS Styling** (`styles.css`)
- `.market-charts`: Main container section
- `.chart-section`: Individual chart sections for crude/equity
- `.chart-header`: Header with controls
- `.charts-grid`: Responsive grid layout for charts
- `.chart-card`: Individual chart containers
- `.chart-summary`: Key metrics display
- Responsive design for mobile devices

#### 3. **JavaScript Functions** (`home.js`)

**Chart Instances:**
```javascript
let crudePriceChartInstance = null;
let crudeIndicatorsChartInstance = null;
let equityPriceChartInstance = null;
let equityIndicatorsChartInstance = null;
```

**Main Functions:**
- `loadCrudeCharts()`: Fetches and displays crude oil data
- `loadEquityCharts()`: Fetches and displays equity data

### Backend Changes

#### 1. **Enhanced DTO** (`LatestCrudeIndicatorDTO.java`)
Added `volume` field to support volume display in charts:
```java
private Long volume;  // Added for chart display
```

#### 2. **Updated Service** (`CrudeOilIndicatorService.java`)
Modified `getLatestIndicators()` to include volume data:
```java
.volume(ohlcv.getVolume())
```

## API Endpoints Used

### Crude Oil
- **GET** `/api/v1/crude/indicators/latest?timeframes={timeframe}&limit={limit}`
  - Parameters:
    - `timeframes`: "4H", "1H", or "15M"
    - `limit`: Number of records (default: 50)
  - Returns: Latest indicators with OHLCV data

### Equity
- **GET** `/api/v1/analysis/prices/{symbol}?days={days}`
  - Parameters:
    - `symbol`: Stock symbol (e.g., "TCS")
    - `days`: Number of days of historical data
  - Returns: Price data array

- **GET** `/api/v1/analysis/indicators/{symbol}`
  - Parameters:
    - `symbol`: Stock symbol
  - Returns: Last 10 technical indicator records

## Usage Guide

### Viewing Crude Oil Charts

1. Navigate to the home page (index.html)
2. Scroll to the "Market Overview with Technical Indicators" section
3. Use the timeframe dropdown to select 4H, 1H, or 15M
4. Click "Refresh" to reload the data
5. View the charts and summary metrics

**Interpreting the Data:**
- **Price Chart**: Shows the price trend with VWAP as a dynamic support/resistance line
- **Indicators Chart**: 
  - RSI above 70 suggests overbought conditions (potential sell)
  - RSI below 30 suggests oversold conditions (potential buy)
  - Volume Ratio above 150% indicates high trading activity

### Viewing Equity Charts

1. Navigate to the home page (index.html)
2. Scroll to the equity analysis section
3. Enter a stock symbol (e.g., TCS, INFY, RELIANCE)
4. Optionally adjust the number of days (default: 30)
5. Click "Load" to fetch and display the charts
6. View the charts and summary metrics

**Interpreting the Data:**
- **Price Chart**: Shows historical price movement with VWAP
- **Indicators Chart**: Same as crude oil analysis
- **Summary Metrics**: Quick glance at current conditions

## Decision-Making Guide

### Bullish Signals (Consider Buying)
✅ RSI < 30 (oversold)
✅ MACD Signal = bullish
✅ Price above VWAP
✅ Volume Ratio > 100% (increasing activity)

### Bearish Signals (Consider Selling)
❌ RSI > 70 (overbought)
❌ MACD Signal = bearish
❌ Price below VWAP
❌ Volume Ratio < 50% (decreasing activity)

### Neutral/Wait Signals
⚠️ RSI between 30-70
⚠️ MACD Signal = neutral
⚠️ Volume Ratio between 50-100%

### Confirmation Strategies
1. **Multiple Timeframe Analysis** (for Crude Oil):
   - Check all three timeframes (4H, 1H, 15M)
   - Stronger signal if all align in same direction

2. **Volume Confirmation**:
   - High volume (>150%) with price movement = strong trend
   - Low volume (<50%) = weak trend, be cautious

3. **VWAP Position**:
   - Price consistently above VWAP = bullish bias
   - Price consistently below VWAP = bearish bias

## Chart Features

### Interactive Elements
- **Hover tooltips**: Hover over any point to see exact values
- **Legend**: Click legend items to show/hide datasets
- **Responsive**: Automatically adjusts to screen size
- **Auto-refresh**: Data updates automatically every 30 seconds

### Visual Indicators
- **Colors**:
  - Blue: Price data
  - Orange: VWAP
  - Purple: RSI
  - Green: Volume Ratio/Bullish signals
  - Red: Bearish signals

## Performance Considerations

- Charts use Chart.js 4.4.0 for optimal performance
- Data is fetched asynchronously to prevent blocking
- Chart instances are properly destroyed and recreated to prevent memory leaks
- Default limit of 50 data points for crude oil to ensure smooth rendering
- Configurable time range for equity data (10-100 days)

## Troubleshooting

### Charts Not Loading

1. **Check API Availability**:
   - Verify system status in the "System Status" section
   - Ensure Crude Oil Service is "Online"

2. **Check Data Availability**:
   - For Crude Oil: Ensure OHLCV data has been loaded
   - For Equity: Verify the stock symbol is valid and has data

3. **Browser Console**:
   - Open browser developer tools (F12)
   - Check console for error messages

### Incomplete Data

1. **Crude Oil**:
   - Run "Load OHLCV Data" from Quick Access section
   - Run "Calculate Indicators" after loading data

2. **Equity**:
   - Ensure price data exists for the symbol
   - Run "Calculate Stock Indicators" if needed

### Chart Display Issues

1. **Clear browser cache** and refresh the page
2. **Check browser compatibility**: Chrome, Firefox, Safari, Edge (latest versions)
3. **Disable browser extensions** that might interfere with JavaScript

## Future Enhancements

### Planned Features
1. **Candlestick charts** for OHLC visualization
2. **Bollinger Bands** overlay on price chart
3. **MACD line chart** instead of just signal display
4. **OBV chart** for crude oil analysis
5. **Moving averages** (SMA 20, 50, 100, 200) overlay
6. **Support/Resistance levels** visualization
7. **Custom date range picker** for both crude and equity
8. **Export chart as image** functionality
9. **Multiple equity symbols** comparison on same chart
10. **Alerts** based on indicator thresholds

### Suggested Improvements
1. Add volume bars on price chart
2. Include ADX for trend strength
3. Add Fibonacci retracement levels
4. Implement chart pattern recognition
5. Add ability to draw trendlines

## Files Modified

1. `/src/main/resources/static/index.html` - Added chart sections and Chart.js library
2. `/src/main/resources/static/css/styles.css` - Added chart styling
3. `/src/main/resources/static/js/home.js` - Added chart loading functions
4. `/src/main/java/com/stockanalyzer/dto/LatestCrudeIndicatorDTO.java` - Added volume field
5. `/src/main/java/com/stockanalyzer/service/CrudeOilIndicatorService.java` - Updated to include volume

## Dependencies

- **Chart.js v4.4.0**: Main charting library
- **Font Awesome 6.4.0**: Icons (already present)
- **Existing API endpoints**: No new backend endpoints required

## Compatibility

- **Browsers**: Chrome 90+, Firefox 88+, Safari 14+, Edge 90+
- **Mobile**: Fully responsive, works on all devices
- **Screen Sizes**: Desktop, tablet, and mobile optimized

## Support

For issues or questions:
1. Check the browser console for errors
2. Verify API endpoint availability
3. Review system status on home page
4. Consult API documentation at `/api/v1/crude/api-info`

---

**Last Updated**: November 21, 2025
**Version**: 1.0.0

