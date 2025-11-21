// ===== Crude Oil Analysis Page JavaScript =====

// Initialize page
document.addEventListener('DOMContentLoaded', () => {
    refreshStats();
    setDefaultDates();
});

// Set default dates
function setDefaultDates() {
    const now = new Date();
    const endDate = now.toISOString().split('T')[0];
    const startDate = new Date(now - 60 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
    
    // Load data dates
    document.getElementById('loadStartDate').value = startDate;
    document.getElementById('loadEndDate').value = endDate;
    
    // Calculate dates (with time)
    const endDateTime = now.toISOString().slice(0, 16);
    const startDateTime = new Date(now - 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 16);
    document.getElementById('calcStartDate').value = startDateTime;
    document.getElementById('calcEndDate').value = endDateTime;
    document.getElementById('downloadStartDate').value = startDateTime;
    document.getElementById('downloadEndDate').value = endDateTime;
}

// Refresh statistics
window.refreshStats = async function() {
    const btn = event?.target;
    if (btn) stockAnalyzer.setLoading(btn, true);
    
    try {
        const data = await stockAnalyzer.fetchAPI('/api/v1/crude/ohlcv-stats?symbol=BRN');
        
        if (data.success) {
            document.getElementById('count4H').textContent = stockAnalyzer.formatLargeNumber(data.counts['4H']);
            document.getElementById('count1H').textContent = stockAnalyzer.formatLargeNumber(data.counts['1H']);
            document.getElementById('count15M').textContent = stockAnalyzer.formatLargeNumber(data.counts['15M']);
            
            const status = data.ready_for_calculation ? 'Ready' : 'Insufficient Data';
            const statusEl = document.getElementById('dataStatus');
            statusEl.textContent = status;
            statusEl.style.color = data.ready_for_calculation ? 'var(--secondary-color)' : 'var(--warning-color)';
        }
    } catch (error) {
        stockAnalyzer.showNotification('Failed to load stats: ' + error.message, 'error');
    } finally {
        if (btn) stockAnalyzer.setLoading(btn, false);
    }
};

// Load OHLCV Data
window.loadOHLCVData = async function() {
    const btn = event.target;
    const resultEl = document.getElementById('loadResult');
    
    stockAnalyzer.setLoading(btn, true);
    resultEl.textContent = 'Loading data from FivePaisa...';
    
    try {
        const symbol = document.getElementById('loadSymbol').value;
        const startDate = document.getElementById('loadStartDate').value;
        const endDate = document.getElementById('loadEndDate').value;
        
        if (!startDate || !endDate) {
            throw new Error('Please select start and end dates');
        }
        
        const params = new URLSearchParams({ symbol, startDate, endDate });
        const data = await stockAnalyzer.fetchAPI(`/api/v1/crude/load-ohlcv-data?${params}`, {
            method: 'POST'
        });
        
        if (data.success) {
            resultEl.textContent = JSON.stringify(data, null, 2);
            stockAnalyzer.showNotification(`Loaded ${data.recordsLoaded} records successfully`, 'success');
            refreshStats();
        } else {
            throw new Error(data.error || 'Failed to load data');
        }
    } catch (error) {
        resultEl.textContent = `Error: ${error.message}`;
        stockAnalyzer.showNotification('Load failed: ' + error.message, 'error');
    } finally {
        stockAnalyzer.setLoading(btn, false);
    }
};

// Calculate Indicators
window.calculateIndicators = async function() {
    const btn = event.target;
    const resultEl = document.getElementById('calcResult');
    
    stockAnalyzer.setLoading(btn, true);
    resultEl.textContent = 'Calculating indicators...';
    
    try {
        const symbol = document.getElementById('calcSymbol').value;
        const timeframes = Array.from(document.querySelectorAll('.checkbox-group input:checked'))
            .map(cb => cb.value);
        const startDate = document.getElementById('calcStartDate').value;
        const endDate = document.getElementById('calcEndDate').value;
        const recalculate = document.getElementById('calcRecalculate').checked;
        
        if (timeframes.length === 0) {
            throw new Error('Please select at least one timeframe');
        }
        
        const requestBody = {
            symbol,
            timeframes,
            recalculate
        };
        
        if (startDate) requestBody.startDate = startDate;
        if (endDate) requestBody.endDate = endDate;
        
        const data = await stockAnalyzer.fetchAPI('/api/v1/crude/calculate-indicators', {
            method: 'POST',
            body: JSON.stringify(requestBody)
        });
        
        if (data.success) {
            resultEl.textContent = JSON.stringify(data, null, 2);
            const total = data.data.totalRecords;
            const time = data.data.executionTimeMs;
            stockAnalyzer.showNotification(`Calculated ${total} indicators in ${time}ms`, 'success');
        } else {
            throw new Error(data.error || 'Calculation failed');
        }
    } catch (error) {
        resultEl.textContent = `Error: ${error.message}`;
        stockAnalyzer.showNotification('Calculation failed: ' + error.message, 'error');
    } finally {
        stockAnalyzer.setLoading(btn, false);
    }
};

// Get Latest Indicators
window.getLatestIndicators = async function() {
    const btn = event.target;
    const resultEl = document.getElementById('latestResult');
    
    stockAnalyzer.setLoading(btn, true);
    resultEl.textContent = 'Fetching latest indicators...';
    
    try {
        const timeframes = Array.from(document.getElementById('latestTimeframes').selectedOptions)
            .map(opt => opt.value).join(',');
        const limit = document.getElementById('latestLimit').value;
        
        if (!timeframes) {
            throw new Error('Please select at least one timeframe');
        }
        
        const params = new URLSearchParams({ timeframes, limit });
        const data = await stockAnalyzer.fetchAPI(`/api/v1/crude/indicators/latest?${params}`);
        
        if (data.success) {
            // Format the output nicely
            let formatted = '';
            for (const [timeframe, indicators] of Object.entries(data.data)) {
                formatted += `\n=== ${timeframe} Timeframe ===\n\n`;
                indicators.forEach((ind, idx) => {
                    formatted += `${idx + 1}. ${stockAnalyzer.formatDisplayDate(ind.timestamp)}\n`;
                    formatted += `   Close: ${stockAnalyzer.formatNumber(ind.close, 2)}\n`;
                    formatted += `   RSI(14): ${stockAnalyzer.formatNumber(ind.rsi14, 2)}\n`;
                    formatted += `   MACD Signal: ${ind.macdCrossoverSignal || 'N/A'}\n`;
                    formatted += `   OBV: ${stockAnalyzer.formatLargeNumber(ind.obv)}\n`;
                    formatted += `   VWAP: ${stockAnalyzer.formatNumber(ind.vwap, 2)}\n`;
                    formatted += `   Volume Ratio: ${stockAnalyzer.formatPercentage(ind.volumeRatio)}\n`;
                    formatted += `   Quality: ${ind.dataQualityFlag}\n\n`;
                });
            }
            resultEl.textContent = formatted;
            stockAnalyzer.showNotification('Latest indicators loaded', 'success');
        } else {
            throw new Error('Failed to fetch indicators');
        }
    } catch (error) {
        resultEl.textContent = `Error: ${error.message}`;
        stockAnalyzer.showNotification('Fetch failed: ' + error.message, 'error');
    } finally {
        stockAnalyzer.setLoading(btn, false);
    }
};

// Download Indicators
window.downloadIndicators = async function() {
    const btn = event.target;
    const resultEl = document.getElementById('downloadResult');
    
    stockAnalyzer.setLoading(btn, true);
    resultEl.textContent = 'Preparing download...';
    
    try {
        const timeframes = Array.from(document.getElementById('downloadTimeframes').selectedOptions)
            .map(opt => opt.value).join(',');
        const startDate = document.getElementById('downloadStartDate').value;
        const endDate = document.getElementById('downloadEndDate').value;
        const quality = Array.from(document.getElementById('downloadQuality').selectedOptions)
            .map(opt => opt.value).join(',');
        const format = document.getElementById('downloadFormat').value;
        
        if (!timeframes) {
            throw new Error('Please select at least one timeframe');
        }
        
        const params = new URLSearchParams({ timeframes, format });
        if (startDate) params.append('startDate', startDate);
        if (endDate) params.append('endDate', endDate);
        if (quality) params.append('dataQuality', quality);
        
        const timestamp = new Date().toISOString().split('T')[0];
        const filename = `crude_oil_indicators_${timestamp}.${format}`;
        
        await stockAnalyzer.downloadFromURL(
            `/api/v1/crude/download-indicators?${params}`,
            filename
        );
        
        resultEl.textContent = `Download started: ${filename}`;
        stockAnalyzer.showNotification('Download started!', 'success');
    } catch (error) {
        resultEl.textContent = `Error: ${error.message}`;
        stockAnalyzer.showNotification('Download failed: ' + error.message, 'error');
    } finally {
        stockAnalyzer.setLoading(btn, false);
    }
};

// Auto-refresh stats every 30 seconds
setInterval(refreshStats, 30000);

