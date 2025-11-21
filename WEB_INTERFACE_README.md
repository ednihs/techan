# Stock Analyzer - Web Interface Documentation

## Overview

A modern, responsive web interface for the Stock Analyzer Platform featuring dark mode, real-time updates, and comprehensive API integration.

## Features

### ✨ Modern Design
- **Responsive Layout** - Works seamlessly on desktop, tablet, and mobile devices
- **Dark Mode** - Toggle between light and dark themes (saved to localStorage)
- **Professional UI** - Clean, modern interface with smooth animations
- **Mobile-First** - Optimized for all screen sizes

### 🎨 Pages

#### 1. Home Page (`index.html`)
- **Dashboard Overview** - System statistics and quick access
- **Feature Cards** - Overview of platform capabilities
- **Quick Actions** - One-click access to common operations
- **System Status** - Real-time health monitoring
- **Auto-Refresh** - Stats update every 30 seconds

#### 2. Crude Oil Analysis (`crude-oil.html`)
- **Data Overview** - OHLCV record counts by timeframe
- **Load OHLCV Data** - Import historical data from FivePaisa
- **Calculate Indicators** - Process technical indicators with volume metrics
- **Latest Indicators** - View most recent analysis
- **Download Data** - Export as CSV or JSON
- **Volume Indicators Guide** - Interpretation help for OBV, VWAP, PVT, etc.

#### 3. Stock Analysis (`stock-analysis.html`)
- **BTST Recommendations** - Buy Today Sell Tomorrow signals
- **Run Analysis** - On-demand technical analysis
- **Detailed Analysis** - Deep dive into specific stocks
- **Technical Indicators** - Comprehensive indicator calculation
- **Bulk Download** - Export multiple stocks at once
- **Reauthentication** - FivePaisa session management

#### 4. Portfolio (`portfolio.html`)
- _Existing portfolio management page retained_

## File Structure

```
src/main/resources/static/
├── index.html                  # Home page
├── crude-oil.html             # Crude oil analysis
├── stock-analysis.html        # Stock analysis
├── portfolio.html             # Portfolio (existing)
├── css/
│   ├── styles.css            # Main stylesheet
│   └── crude-oil.css         # Page-specific styles
└── js/
    ├── main.js               # Core functionality
    ├── home.js               # Home page logic
    ├── crude-oil.js          # Crude oil page logic
    └── stock-analysis.js     # Stock analysis logic
```

## Technologies Used

- **HTML5** - Semantic markup
- **CSS3** - Modern styling with CSS variables
- **JavaScript (ES6+)** - Modern JavaScript features
- **Font Awesome 6** - Icon library
- **Fetch API** - HTTP requests
- **LocalStorage** - Theme persistence

## API Integration

### Crude Oil APIs
- `POST /api/v1/crude/load-ohlcv-data` - Load OHLCV data
- `GET /api/v1/crude/ohlcv-stats` - Get data statistics
- `POST /api/v1/crude/calculate-indicators` - Calculate indicators
- `GET /api/v1/crude/indicators/latest` - Get latest indicators
- `GET /api/v1/crude/download-indicators` - Download data
- `GET /api/v1/crude/health` - Health check

### Stock Analysis APIs
- `GET /api/v1/analysis/btst/recommendations` - BTST recommendations
- `GET /api/v1/analysis/btst/run` - Run BTST analysis
- `GET /api/v1/analysis/btst/detailed/{symbol}` - Detailed BTST
- `GET /api/v1/analysis/technical/run` - Calculate indicators
- `GET /api/v1/analysis/indicators/{symbol}` - Stock indicators
- `GET /api/v1/analysis/indicators/all/json` - All indicators (JSON)
- `GET /api/v1/analysis/indicators/all` - All indicators (CSV)
- `GET /api/v1/analysis/indicators/bulk/download` - Bulk download
- `GET /reauthenticate` - Reauthenticate

### System APIs
- `GET /actuator/health` - Application health
- `GET /actuator/metrics` - System metrics

## Key Features Explained

### Dark Mode
- Toggle between light and dark themes
- Preference saved to localStorage
- Smooth transition animations
- Consistent across all pages

### Responsive Navigation
- Hamburger menu on mobile devices
- Smooth slide-out navigation
- Active page highlighting
- Quick access to all sections

### Loading States
- Visual feedback for all operations
- Disabled buttons during processing
- Spinner animations
- Clear status messages

### Notification System
- Toast notifications for user feedback
- Success, error, warning, and info types
- Auto-dismiss after 5 seconds
- Slide-in/out animations

### Auto-Refresh
- Home stats refresh every 30 seconds
- System status refresh every 60 seconds
- Crude oil stats refresh every 30 seconds
- Manual refresh buttons available

### Data Export
- CSV format for Excel/analysis tools
- JSON format for programmatic access
- Customizable date ranges
- Multiple timeframe support
- Data quality filtering

## Usage Examples

### Load Crude Oil Data
1. Navigate to "Crude Oil Analysis" page
2. Select date range (defaults to last 60 days)
3. Click "Load Data"
4. Wait for confirmation
5. Check "Data Overview" for updated counts

### Calculate Indicators
1. Ensure OHLCV data is loaded (≥200 records per timeframe)
2. Select timeframes (4H, 1H, 15M)
3. Optionally set date range
4. Check "Recalculate" to overwrite existing
5. Click "Calculate Indicators"
6. View execution time and results

### Download Analysis
1. Select desired timeframes
2. Set date range (optional)
3. Choose data quality (Fresh/Recent/Stale)
4. Select format (CSV or JSON)
5. Click "Download"
6. File downloads automatically

### View Latest Indicators
1. Select timeframes
2. Set limit (number of recent records)
3. Click "View Latest"
4. Review formatted output

### BTST Recommendations
1. Navigate to "Stock Analysis" page
2. Select date and recommendation type (BUY/HOLD/AVOID)
3. Click "Get Recommendations"
4. View list of symbols with confidence scores

## Customization

### Modify Colors
Edit CSS variables in `styles.css`:

```css
:root {
    --primary-color: #3498db;
    --secondary-color: #2ecc71;
    --danger-color: #e74c3c;
    --warning-color: #f39c12;
    /* ... more variables */
}
```

### Add New API Endpoints
1. Add UI elements in HTML
2. Create handler function in corresponding JS file
3. Use `stockAnalyzer.fetchAPI()` for requests
4. Display results and show notifications

### Extend Functionality
The `stockAnalyzer` global object provides utility functions:
- `fetchAPI(endpoint, options)` - API calls
- `showNotification(message, type)` - Notifications
- `formatDate(date)` - Date formatting
- `formatNumber(num, decimals)` - Number formatting
- `downloadFile(data, filename, type)` - File downloads
- `setLoading(element, isLoading)` - Loading states

## Browser Compatibility

- ✅ Chrome 90+
- ✅ Firefox 88+
- ✅ Safari 14+
- ✅ Edge 90+
- ✅ Mobile browsers (iOS Safari, Chrome Mobile)

## Performance

- **Lazy Loading** - Sections load as visible
- **Debounced Events** - Optimized event handlers
- **Efficient DOM** - Minimal reflows
- **CSS Animations** - Hardware-accelerated
- **Code Splitting** - Page-specific JavaScript

## Accessibility

- **Semantic HTML** - Proper element usage
- **ARIA Labels** - Screen reader support
- **Keyboard Navigation** - Full keyboard support
- **Focus Indicators** - Visible focus states
- **Color Contrast** - WCAG AA compliant

## Development

### Local Development
1. Start Spring Boot application
2. Navigate to `http://localhost:8080`
3. Interface loads automatically

### Adding New Pages
1. Create HTML file in `static/` directory
2. Create corresponding CSS in `static/css/`
3. Create corresponding JS in `static/js/`
4. Add navigation link in `navbar`
5. Update footer links

### Debugging
- Open browser DevTools (F12)
- Check Console for errors
- Network tab shows API calls
- Sources tab for JavaScript debugging

## Security Considerations

- **No Sensitive Data** - API keys server-side only
- **HTTPS** - Use HTTPS in production
- **CORS** - Configure appropriately
- **Input Validation** - Client and server-side
- **Session Management** - Secure token handling

## Future Enhancements

### Planned Features
- 📊 **Interactive Charts** - Chart.js or D3.js integration
- 🔔 **Real-time Alerts** - WebSocket notifications
- 📱 **PWA Support** - Progressive Web App
- 🔍 **Advanced Filters** - More granular data filtering
- 💾 **Saved Queries** - Store favorite searches
- 📈 **Historical Charts** - Price and volume visualization
- 🎯 **Watchlists** - Custom stock tracking
- 🔐 **User Authentication** - Multi-user support

### Enhancement Ideas
- Add comparison charts for indicators
- Implement drag-and-drop file uploads
- Add export to PDF functionality
- Create dashboard customization
- Add real-time price tickers
- Implement advanced technical patterns
- Add backtesting visualizations

## Troubleshooting

### Issue: Page Not Loading
- Check application is running on port 8080
- Verify Spring Boot started successfully
- Check browser console for errors
- Clear browser cache

### Issue: API Calls Failing
- Verify backend services are running
- Check network tab for HTTP status codes
- Ensure database is connected
- Review application logs

### Issue: Dark Mode Not Persisting
- Check localStorage is enabled
- Verify no browser extensions blocking storage
- Try in incognito mode to test

### Issue: Download Not Starting
- Check popup blocker settings
- Verify file download permissions
- Check browser download settings
- Review console for errors

## Support

For issues or questions:
1. Check browser console for errors
2. Review application logs
3. Verify API endpoints are accessible
4. Check system health: `/actuator/health`

## Version History

- **v1.0.0** (Nov 2025) - Initial release
  - Modern responsive interface
  - Dark mode support
  - Crude oil analysis integration
  - Stock analysis consolidation
  - Auto-refresh capabilities

---

**Last Updated:** November 20, 2025  
**Maintained by:** Stock Analyzer Team

