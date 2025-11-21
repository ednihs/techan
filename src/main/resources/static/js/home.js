// ===== Home Page Specific JavaScript =====

// Update stats on page load
document.addEventListener('DOMContentLoaded', async () => {
    await loadSystemStatus();
    await loadStats();
});

// Load system status
async function loadSystemStatus() {
    // Check API Health
    try {
        await stockAnalyzer.fetchAPI('/actuator/health');
        updateStatus('apiStatus', 'online', 'All systems operational');
        updateStatus('dbStatus', 'online', 'Database connected');
    } catch (error) {
        updateStatus('apiStatus', 'offline', 'Service unavailable');
        updateStatus('dbStatus', 'offline', 'Connection failed');
    }

    // Check Crude Oil Service
    try {
        await stockAnalyzer.fetchAPI('/api/v1/crude/health');
        updateStatus('crudeStatus', 'online', 'Service operational');
    } catch (error) {
        updateStatus('crudeStatus', 'offline', 'Service unavailable');
    }
}

function updateStatus(elementId, status, message) {
    const badge = document.getElementById(elementId);
    const details = document.getElementById(elementId.replace('Status', 'Details'));
    
    if (badge) {
        badge.className = `status-badge ${status}`;
        badge.textContent = status === 'online' ? 'Online' : 'Offline';
    }
    
    if (details) {
        details.textContent = message;
    }
}

// Load statistics
async function loadStats() {
    try {
        // Get crude oil stats
        const crudeStats = await stockAnalyzer.fetchAPI('/api/v1/crude/ohlcv-stats?symbol=BRN');
        
        if (crudeStats.success) {
            const counts = crudeStats.counts;
            const total = Object.values(counts).reduce((a, b) => a + b, 0);
            document.getElementById('totalRecords').textContent = stockAnalyzer.formatLargeNumber(total);
        }
    } catch (error) {
        console.error('Error loading stats:', error);
        document.getElementById('totalRecords').textContent = '-';
    }

    // Count active indicators (rough estimate)
    document.getElementById('activeIndicators').textContent = '15+';
    
    // Last update time
    document.getElementById('lastUpdate').textContent = 'Live';
}

// Quick action handlers
window.quickAction = async function(action) {
    let resultElement;
    if (action.startsWith('crude')) {
        resultElement = document.getElementById('crudeResult');
    } else if (action === 'fetchLiveData' || action === 'downloadBhavcopy') {
        resultElement = document.getElementById('dataResult');
    } else {
        resultElement = document.getElementById('stockResult');
    }
    
    resultElement.textContent = 'Processing...';
    resultElement.classList.add('loading');

    try {
        let result;
        
        switch (action) {
            case 'loadCrudeData':
                result = await loadCrudeData();
                break;
            case 'calculateCrudeIndicators':
                result = await calculateCrudeIndicators();
                break;
            case 'viewCrudeLatest':
                result = await viewCrudeLatest();
                break;
            case 'runBTSTAnalysis':
                result = await runBTSTAnalysis();
                break;
            case 'calculateStockIndicators':
                result = await calculateStockIndicators();
                break;
            case 'viewBTSTRecommendations':
                result = await viewBTSTRecommendations();
                break;
            case 'fetchLiveData':
                result = await fetchLiveData();
                break;
            case 'downloadBhavcopy':
                result = await downloadBhavcopy();
                break;
            default:
                result = { error: 'Unknown action' };
        }

        if (result.success) {
            resultElement.innerHTML = `<div style="color: var(--secondary-color);">
                <i class="fas fa-check-circle"></i> ${result.message}
            </div>`;
            stockAnalyzer.showNotification(result.message, 'success');
        } else {
            throw new Error(result.error || 'Operation failed');
        }
    } catch (error) {
        resultElement.innerHTML = `<div style="color: var(--danger-color);">
            <i class="fas fa-exclamation-circle"></i> ${error.message}
        </div>`;
        stockAnalyzer.showNotification('Error: ' + error.message, 'error');
    } finally {
        resultElement.classList.remove('loading');
    }
};

// Crude Oil Actions
async function loadCrudeData() {
    const endDate = new Date().toISOString().split('T')[0];
    const startDate = new Date(Date.now() - 60 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
    
    const data = await stockAnalyzer.fetchAPI(
        `/api/v1/crude/load-ohlcv-data?symbol=BRN&startDate=${startDate}&endDate=${endDate}`,
        { method: 'POST' }
    );
    
    if (data.success) {
        return {
            success: true,
            message: `Loaded ${data.recordsLoaded} OHLCV records`
        };
    }
    
    throw new Error(data.error || 'Failed to load data');
}

async function calculateCrudeIndicators() {
    const data = await stockAnalyzer.fetchAPI('/api/v1/crude/calculate-indicators', {
        method: 'POST',
        body: JSON.stringify({
            symbol: 'BRN',
            timeframes: ['4H', '1H', '15M'],
            recalculate: false
        })
    });
    
    if (data.success) {
        const total = data.data.totalRecords;
        return {
            success: true,
            message: `Calculated ${total} indicators in ${data.data.executionTimeMs}ms`
        };
    }
    
    throw new Error(data.error || 'Calculation failed');
}

async function viewCrudeLatest() {
    const data = await stockAnalyzer.fetchAPI('/api/v1/crude/indicators/latest?timeframes=4H&limit=1');
    
    if (data.success && data.data['4H'] && data.data['4H'].length > 0) {
        const latest = data.data['4H'][0];
        return {
            success: true,
            message: `Latest: Close=${latest.close}, RSI=${latest.rsi14?.toFixed(2)}, Volume Ratio=${latest.volumeRatio?.toFixed(2)}%`
        };
    }
    
    throw new Error('No data available');
}

// Stock Analysis Actions
async function runBTSTAnalysis() {
    const date = new Date().toISOString().split('T')[0];
    const data = await stockAnalyzer.fetchAPI(`/api/v1/analysis/btst/run?date=${date}`);
    
    if (data && Array.isArray(data)) {
        return {
            success: true,
            message: `Analysis complete. Found ${data.length} recommendations`
        };
    }
    
    throw new Error('Analysis failed');
}

async function calculateStockIndicators() {
    const date = new Date().toISOString().split('T')[0];
    const data = await stockAnalyzer.fetchAPI(`/api/v1/analysis/technical/run?date=${date}`);
    
    if (data && Array.isArray(data)) {
        return {
            success: true,
            message: `Calculated indicators for ${data.length} stocks`
        };
    }
    
    throw new Error('Calculation failed');
}

async function viewBTSTRecommendations() {
    const data = await stockAnalyzer.fetchAPI('/api/v1/analysis/btst/recommendations?recommendation=BUY');
    
    if (data && Array.isArray(data)) {
        const topRecs = data.slice(0, 3).map(r => r.symbol).join(', ');
        return {
            success: true,
            message: `Top recommendations: ${topRecs || 'None'}`
        };
    }
    
    throw new Error('Failed to fetch recommendations');
}

// New Data Management Actions
async function fetchLiveData() {
    const data = await stockAnalyzer.fetchAPI('/api/v1/analysis/fetch-live-data');
    
    if (typeof data === 'string' && data.includes('successfully')) {
        return {
            success: true,
            message: 'Live data fetched successfully'
        };
    }
    
    throw new Error(data || 'Failed to fetch live data');
}

async function downloadBhavcopy() {
    const date = new Date().toISOString().split('T')[0];
    const data = await stockAnalyzer.fetchAPI(`/api/v1/analysis/download-bhavcopy?date=${date}`);
    
    if (typeof data === 'string' && data.includes('successfully')) {
        return {
            success: true,
            message: `Bhavcopy downloaded for ${date}`
        };
    }
    
    throw new Error(data || 'Failed to download bhavcopy');
}

// Auto-refresh stats every 30 seconds
setInterval(loadStats, 30000);

// Auto-refresh system status every minute
setInterval(loadSystemStatus, 60000);

