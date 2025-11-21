# Quick Start Guide - Interactive Charts

## What's New? 📊

The home page (`index.html`) now includes **interactive charts** for both **Crude Oil** and **Equity** analysis with technical indicators to help you make better trading decisions!

## Quick Access

### 🛢️ Crude Oil Charts

1. **Open**: Navigate to `http://localhost:8080/` (home page)
2. **Scroll down** to "Market Overview with Technical Indicators"
3. **Select timeframe**: Choose 4H, 1H, or 15M from the dropdown
4. **Click Refresh**: Load the latest data
5. **View insights**: Check the summary panel for key metrics

**What You See:**
- 📈 **Price & Volume Chart**: Price trend with VWAP overlay
- 📊 **Technical Indicators Chart**: RSI and Volume Ratio
- 📋 **Summary Panel**: Latest Close, RSI, MACD Signal, Volume Ratio, VWAP

### 📈 Equity Charts

1. **Open**: Navigate to `http://localhost:8080/` (home page)
2. **Scroll down** to the Equity Analysis section
3. **Enter symbol**: Type any stock symbol (e.g., TCS, INFY, RELIANCE)
4. **Set time range**: Choose number of days (default: 30)
5. **Click Load**: Fetch and display the data

**What You See:**
- 📈 **Price & Volume Chart**: Historical price with VWAP
- 📊 **Technical Indicators Chart**: RSI and Volume Ratio
- 📋 **Summary Panel**: Latest Close, RSI, MACD, Volume Ratio, VWAP

## Quick Decision Guide 🎯

### 🟢 Buy Signals
- RSI < 30 (oversold)
- MACD = bullish (green)
- Price > VWAP
- Volume Ratio > 100%

### 🔴 Sell Signals
- RSI > 70 (overbought)
- MACD = bearish (red)
- Price < VWAP
- Volume Ratio < 50%

### 🟡 Wait/Neutral
- RSI between 30-70
- MACD = neutral
- Mixed signals across indicators

## Understanding the Indicators

### RSI (Relative Strength Index)
- **Range**: 0-100
- **Overbought**: > 70 (might drop soon)
- **Oversold**: < 30 (might rise soon)
- **Neutral**: 30-70

### MACD Signal
- **Bullish**: 🟢 Buy pressure
- **Bearish**: 🔴 Sell pressure
- **Neutral**: 🟡 No clear direction

### Volume Ratio
- **> 150%**: Very high volume (strong conviction)
- **100-150%**: Above average (moderate activity)
- **50-100%**: Below average (weak activity)
- **< 50%**: Very low volume (avoid trading)

### VWAP (Volume Weighted Average Price)
- **Price > VWAP**: Bullish (buyers in control)
- **Price < VWAP**: Bearish (sellers in control)
- Acts as dynamic support/resistance

## Example Workflows

### Scenario 1: Checking Crude Oil Before Trading
1. Open home page
2. Select **1H timeframe** for intraday trading
3. Check RSI:
   - If < 30: Consider buying
   - If > 70: Consider selling
4. Confirm with MACD signal
5. Check Volume Ratio for strength
6. Compare price to VWAP

### Scenario 2: Analyzing a Stock
1. Enter symbol (e.g., **TCS**)
2. Set days to **30** for monthly view
3. Click **Load**
4. Check recent price trend
5. Look at RSI trajectory
6. Confirm with Volume Ratio
7. Compare current price to VWAP

### Scenario 3: Multi-Timeframe Analysis (Crude Oil)
1. Check **4H** timeframe for overall trend
2. Check **1H** timeframe for intermediate trend
3. Check **15M** timeframe for entry/exit timing
4. All three bullish? Strong buy signal!
5. Mixed signals? Wait for confirmation

## Tips for Best Results 💡

### ✅ Do's
- Compare multiple timeframes (crude oil)
- Look for volume confirmation
- Use VWAP as reference
- Wait for clear signals
- Check multiple indicators together

### ❌ Don'ts
- Trade on a single indicator
- Ignore volume data
- Act on low volume moves
- Trade during neutral RSI without confirmation
- Forget to check VWAP position

## Troubleshooting

### "No data available"
**Solution**: 
1. For Crude Oil: Run "Load OHLCV Data" from Quick Access
2. For Crude Oil: Run "Calculate Indicators"
3. For Equity: Verify stock symbol is correct
4. Check system status at top of page

### Charts not loading
**Solution**:
1. Refresh the page (Ctrl+F5)
2. Check browser console for errors (F12)
3. Verify API services are online (System Status section)
4. Clear browser cache

### Incomplete indicators
**Solution**:
1. For Crude Oil: Ensure at least 200 candles are loaded
2. For Equity: Run "Calculate Stock Indicators" from Quick Access
3. Check "Total Records" in hero stats

## Examples of Good Trading Decisions

### Example 1: Strong Buy Signal
```
Crude Oil (1H Timeframe):
- RSI: 28 🟢 (oversold)
- MACD: bullish 🟢
- Price vs VWAP: Above (+2%) 🟢
- Volume Ratio: 165% 🟢
→ Strong buy signal! All indicators align.
```

### Example 2: Strong Sell Signal
```
TCS (Equity):
- RSI: 75 🔴 (overbought)
- MACD: bearish 🔴
- Price vs VWAP: Below (-1.5%) 🔴
- Volume Ratio: 140% 🔴
→ Strong sell signal with high volume confirmation.
```

### Example 3: Wait/Unclear
```
RELIANCE (Equity):
- RSI: 55 🟡 (neutral)
- MACD: neutral 🟡
- Price vs VWAP: At VWAP (0%) 🟡
- Volume Ratio: 85% 🟡
→ Wait for clearer signals before trading.
```

## Advanced Usage

### Combining with Other Features

1. **Use with BTST Recommendations**:
   - View charts for stocks in BTST recommendations
   - Confirm signals before following recommendations

2. **Portfolio Tracking**:
   - Monitor your holdings with charts
   - Set alerts at key RSI levels

3. **Data Export**:
   - Export detailed data via download buttons
   - Analyze in Excel/Python for custom strategies

## System Requirements

- Modern browser (Chrome, Firefox, Safari, Edge)
- JavaScript enabled
- Internet connection for real-time data
- Backend server running on localhost:8080

## Need Help?

1. Check **System Status** section on home page
2. Review **API Documentation**: `/api/v1/crude/api-info`
3. Check **Health Endpoint**: `/actuator/health`
4. View browser console for detailed errors (F12)

---

## What Changed Technically?

### Frontend
- Added Chart.js 4.4.0 library
- New chart sections in `index.html`
- CSS styling in `styles.css`
- Chart functions in `home.js`

### Backend
- Enhanced `LatestCrudeIndicatorDTO` with volume field
- Updated `CrudeOilIndicatorService` to include volume

### Files Modified
- `index.html` - Chart sections
- `styles.css` - Chart styling
- `home.js` - Chart logic
- `LatestCrudeIndicatorDTO.java` - Added volume
- `CrudeOilIndicatorService.java` - Include volume data

---

**Ready to trade smarter? Open the home page and start analyzing!** 🚀

