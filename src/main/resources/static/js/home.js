// ===== Home Page Specific JavaScript =====

// Chart instances
let crudePriceChartInstance = null;
let crudeIndicatorsChartInstance = null;
let equityPriceChartInstance = null;
let equityIndicatorsChartInstance = null;

// Update stats on page load
document.addEventListener('DOMContentLoaded', async () => {
    await loadSystemStatus();
    await loadStats();
    await loadCrudeCharts();
    await loadEquityCharts();
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

// ===== Chart Functions =====

// Load Crude Oil Charts
async function loadCrudeCharts() {
    const timeframe = document.getElementById('crudeTimeframe')?.value || '1H';
    const limit = 50;
    
    try {
        // Fetch latest indicators data
        const response = await stockAnalyzer.fetchAPI(`/api/v1/crude/indicators/latest?timeframes=${timeframe}&limit=${limit}`);
        
        if (!response.success || !response.data || !response.data[timeframe]) {
            console.warn('No crude oil data available');
            return;
        }
        
        const data = response.data[timeframe].reverse(); // Oldest to newest
        
        if (data.length === 0) {
            console.warn('Empty crude oil data');
            return;
        }
        
        // Extract data for charts
        const timestamps = data.map(d => new Date(d.timestamp).toLocaleString('en-US', { 
            month: 'short', 
            day: 'numeric', 
            hour: '2-digit', 
            minute: '2-digit' 
        }));
        const closes = data.map(d => d.close || 0);
        const volumes = data.map(d => d.volume || 0);
        const rsi = data.map(d => d.rsi14 || null);
        const vwap = data.map(d => d.vwap || null);
        const volumeRatio = data.map(d => d.volumeRatio || null);
        
        // Update summary
        const latest = data[data.length - 1];
        document.getElementById('crudeLatestClose').textContent = latest.close?.toFixed(2) || '-';
        document.getElementById('crudeLatestRSI').textContent = latest.rsi14?.toFixed(2) || '-';
        document.getElementById('crudeLatestMACD').textContent = latest.macdCrossoverSignal || '-';
        document.getElementById('crudeLatestVolRatio').textContent = latest.volumeRatio ? `${latest.volumeRatio.toFixed(2)}%` : '-';
        document.getElementById('crudeLatestVWAP').textContent = latest.vwap?.toFixed(2) || '-';
        
        // Apply color coding to RSI
        const rsiElement = document.getElementById('crudeLatestRSI');
        const rsiValue = latest.rsi14;
        if (rsiValue >= 70) {
            rsiElement.className = 'summary-value negative';
        } else if (rsiValue <= 30) {
            rsiElement.className = 'summary-value positive';
        } else {
            rsiElement.className = 'summary-value neutral';
        }
        
        // Apply color to MACD signal
        const macdElement = document.getElementById('crudeLatestMACD');
        if (latest.macdCrossoverSignal === 'bullish') {
            macdElement.className = 'summary-value positive';
        } else if (latest.macdCrossoverSignal === 'bearish') {
            macdElement.className = 'summary-value negative';
        } else {
            macdElement.className = 'summary-value neutral';
        }
        
        // Create Price & Volume Chart
        const ctxPrice = document.getElementById('crudePriceChart');
        if (ctxPrice) {
            if (crudePriceChartInstance) {
                crudePriceChartInstance.destroy();
            }
            
            crudePriceChartInstance = new Chart(ctxPrice, {
                type: 'line',
                data: {
                    labels: timestamps,
                    datasets: [
                        {
                            label: 'Close Price',
                            data: closes,
                            borderColor: '#3498db',
                            backgroundColor: 'rgba(52, 152, 219, 0.1)',
                            borderWidth: 2,
                            yAxisID: 'y',
                            tension: 0.1
                        },
                        {
                            label: 'VWAP',
                            data: vwap,
                            borderColor: '#f39c12',
                            backgroundColor: 'transparent',
                            borderWidth: 2,
                            borderDash: [5, 5],
                            yAxisID: 'y',
                            pointRadius: 0
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: true,
                    interaction: {
                        mode: 'index',
                        intersect: false,
                    },
                    plugins: {
                        legend: {
                            position: 'top',
                        },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    let label = context.dataset.label || '';
                                    if (label) {
                                        label += ': ';
                                    }
                                    if (context.parsed.y !== null) {
                                        label += context.parsed.y.toFixed(2);
                                    }
                                    return label;
                                }
                            }
                        }
                    },
                    scales: {
                        y: {
                            type: 'linear',
                            display: true,
                            position: 'left',
                            title: {
                                display: true,
                                text: 'Price'
                            }
                        }
                    }
                }
            });
        }
        
        // Create Indicators Chart
        const ctxIndicators = document.getElementById('crudeIndicatorsChart');
        if (ctxIndicators) {
            if (crudeIndicatorsChartInstance) {
                crudeIndicatorsChartInstance.destroy();
            }
            
            crudeIndicatorsChartInstance = new Chart(ctxIndicators, {
                type: 'line',
                data: {
                    labels: timestamps,
                    datasets: [
                        {
                            label: 'RSI (14)',
                            data: rsi,
                            borderColor: '#9b59b6',
                            backgroundColor: 'rgba(155, 89, 182, 0.1)',
                            borderWidth: 2,
                            yAxisID: 'y',
                            tension: 0.1
                        },
                        {
                            label: 'Volume Ratio (%)',
                            data: volumeRatio,
                            borderColor: '#2ecc71',
                            backgroundColor: 'rgba(46, 204, 113, 0.1)',
                            borderWidth: 2,
                            yAxisID: 'y1',
                            tension: 0.1
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: true,
                    interaction: {
                        mode: 'index',
                        intersect: false,
                    },
                    plugins: {
                        legend: {
                            position: 'top',
                        },
                        annotation: {
                            annotations: {
                                line1: {
                                    type: 'line',
                                    yMin: 70,
                                    yMax: 70,
                                    borderColor: '#e74c3c',
                                    borderWidth: 1,
                                    borderDash: [5, 5],
                                    label: {
                                        content: 'Overbought',
                                        enabled: true
                                    }
                                },
                                line2: {
                                    type: 'line',
                                    yMin: 30,
                                    yMax: 30,
                                    borderColor: '#2ecc71',
                                    borderWidth: 1,
                                    borderDash: [5, 5],
                                    label: {
                                        content: 'Oversold',
                                        enabled: true
                                    }
                                }
                            }
                        }
                    },
                    scales: {
                        y: {
                            type: 'linear',
                            display: true,
                            position: 'left',
                            title: {
                                display: true,
                                text: 'RSI'
                            },
                            min: 0,
                            max: 100
                        },
                        y1: {
                            type: 'linear',
                            display: true,
                            position: 'right',
                            title: {
                                display: true,
                                text: 'Volume Ratio (%)'
                            },
                            grid: {
                                drawOnChartArea: false,
                            }
                        }
                    }
                }
            });
        }
        
    } catch (error) {
        console.error('Error loading crude oil charts:', error);
        stockAnalyzer.showNotification('Failed to load crude oil charts', 'error');
    }
}

// Load Equity Charts
async function loadEquityCharts() {
    const symbol = document.getElementById('equitySymbol')?.value || 'TCS';
    const days = parseInt(document.getElementById('equityDays')?.value || '30');
    
    if (!symbol) {
        stockAnalyzer.showNotification('Please enter a stock symbol', 'error');
        return;
    }
    
    try {
        // Fetch price data
        const priceData = await stockAnalyzer.fetchAPI(`/api/v1/analysis/prices/${symbol}?days=${days}`);
        
        if (!priceData || priceData.length === 0) {
            stockAnalyzer.showNotification(`No data available for ${symbol}`, 'warning');
            return;
        }
        
        // Fetch indicators for the last 10 records
        const indicatorData = await stockAnalyzer.fetchAPI(`/api/v1/analysis/indicators/${symbol}`);
        
        // Prepare data
        const timestamps = priceData.map(d => new Date(d.tradeDate).toLocaleDateString('en-US', { 
            month: 'short', 
            day: 'numeric' 
        }));
        const closes = priceData.map(d => parseFloat(d.closePrice) || 0);
        const volumes = priceData.map(d => d.volume || 0);
        
        // Map indicators to match price data dates
        const indicatorMap = {};
        if (indicatorData && Array.isArray(indicatorData)) {
            indicatorData.forEach(ind => {
                const dateKey = new Date(ind.calculationDate).toISOString().split('T')[0];
                indicatorMap[dateKey] = ind;
            });
        }
        
        const rsi = priceData.map(d => {
            const dateKey = new Date(d.tradeDate).toISOString().split('T')[0];
            return indicatorMap[dateKey]?.rsi14 || null;
        });
        
        const vwap = priceData.map(d => {
            const dateKey = new Date(d.tradeDate).toISOString().split('T')[0];
            return indicatorMap[dateKey]?.vwap || null;
        });
        
        const volumeRatio = priceData.map(d => {
            const dateKey = new Date(d.tradeDate).toISOString().split('T')[0];
            return indicatorMap[dateKey]?.volumeRatio ? indicatorMap[dateKey].volumeRatio * 100 : null;
        });
        
        const macdHistogram = priceData.map(d => {
            const dateKey = new Date(d.tradeDate).toISOString().split('T')[0];
            return indicatorMap[dateKey]?.macdHistogram || null;
        });
        
        // Update summary
        const latest = priceData[priceData.length - 1];
        const latestDateKey = new Date(latest.tradeDate).toISOString().split('T')[0];
        const latestIndicator = indicatorMap[latestDateKey] || {};
        
        document.getElementById('equityLatestClose').textContent = parseFloat(latest.closePrice)?.toFixed(2) || '-';
        document.getElementById('equityLatestRSI').textContent = latestIndicator.rsi14?.toFixed(2) || '-';
        
        // Determine MACD signal from histogram
        let macdSignal = 'neutral';
        if (latestIndicator.macdHistogram > 0) {
            macdSignal = 'bullish';
        } else if (latestIndicator.macdHistogram < 0) {
            macdSignal = 'bearish';
        }
        document.getElementById('equityLatestMACD').textContent = macdSignal;
        document.getElementById('equityLatestVolRatio').textContent = latestIndicator.volumeRatio ? `${(latestIndicator.volumeRatio * 100).toFixed(2)}%` : '-';
        document.getElementById('equityLatestVWAP').textContent = latestIndicator.vwap?.toFixed(2) || '-';
        
        // Apply color coding
        const equityRsiElement = document.getElementById('equityLatestRSI');
        const equityRsiValue = latestIndicator.rsi14;
        if (equityRsiValue >= 70) {
            equityRsiElement.className = 'summary-value negative';
        } else if (equityRsiValue <= 30) {
            equityRsiElement.className = 'summary-value positive';
        } else {
            equityRsiElement.className = 'summary-value neutral';
        }
        
        const equityMacdElement = document.getElementById('equityLatestMACD');
        if (macdSignal === 'bullish') {
            equityMacdElement.className = 'summary-value positive';
        } else if (macdSignal === 'bearish') {
            equityMacdElement.className = 'summary-value negative';
        } else {
            equityMacdElement.className = 'summary-value neutral';
        }
        
        // Create Price & Volume Chart
        const ctxPrice = document.getElementById('equityPriceChart');
        if (ctxPrice) {
            if (equityPriceChartInstance) {
                equityPriceChartInstance.destroy();
            }
            
            equityPriceChartInstance = new Chart(ctxPrice, {
                type: 'line',
                data: {
                    labels: timestamps,
                    datasets: [
                        {
                            label: 'Close Price',
                            data: closes,
                            borderColor: '#3498db',
                            backgroundColor: 'rgba(52, 152, 219, 0.1)',
                            borderWidth: 2,
                            yAxisID: 'y',
                            tension: 0.1,
                            fill: true
                        },
                        {
                            label: 'VWAP',
                            data: vwap,
                            borderColor: '#f39c12',
                            backgroundColor: 'transparent',
                            borderWidth: 2,
                            borderDash: [5, 5],
                            yAxisID: 'y',
                            pointRadius: 0
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: true,
                    interaction: {
                        mode: 'index',
                        intersect: false,
                    },
                    plugins: {
                        legend: {
                            position: 'top',
                        },
                        title: {
                            display: true,
                            text: `${symbol} - Price Analysis`
                        }
                    },
                    scales: {
                        y: {
                            type: 'linear',
                            display: true,
                            position: 'left',
                            title: {
                                display: true,
                                text: 'Price (₹)'
                            }
                        }
                    }
                }
            });
        }
        
        // Create Indicators Chart
        const ctxIndicators = document.getElementById('equityIndicatorsChart');
        if (ctxIndicators) {
            if (equityIndicatorsChartInstance) {
                equityIndicatorsChartInstance.destroy();
            }
            
            equityIndicatorsChartInstance = new Chart(ctxIndicators, {
                type: 'line',
                data: {
                    labels: timestamps,
                    datasets: [
                        {
                            label: 'RSI (14)',
                            data: rsi,
                            borderColor: '#9b59b6',
                            backgroundColor: 'rgba(155, 89, 182, 0.1)',
                            borderWidth: 2,
                            yAxisID: 'y',
                            tension: 0.1
                        },
                        {
                            label: 'Volume Ratio (%)',
                            data: volumeRatio,
                            borderColor: '#2ecc71',
                            backgroundColor: 'rgba(46, 204, 113, 0.1)',
                            borderWidth: 2,
                            yAxisID: 'y1',
                            tension: 0.1
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: true,
                    interaction: {
                        mode: 'index',
                        intersect: false,
                    },
                    plugins: {
                        legend: {
                            position: 'top',
                        },
                        title: {
                            display: true,
                            text: `${symbol} - Technical Indicators`
                        }
                    },
                    scales: {
                        y: {
                            type: 'linear',
                            display: true,
                            position: 'left',
                            title: {
                                display: true,
                                text: 'RSI'
                            },
                            min: 0,
                            max: 100
                        },
                        y1: {
                            type: 'linear',
                            display: true,
                            position: 'right',
                            title: {
                                display: true,
                                text: 'Volume Ratio (%)'
                            },
                            grid: {
                                drawOnChartArea: false,
                            }
                        }
                    }
                }
            });
        }
        
        stockAnalyzer.showNotification(`Charts loaded for ${symbol}`, 'success');
        
    } catch (error) {
        console.error('Error loading equity charts:', error);
        stockAnalyzer.showNotification(`Failed to load charts for ${symbol}`, 'error');
    }
}

// Make functions globally available
window.loadCrudeCharts = loadCrudeCharts;
window.loadEquityCharts = loadEquityCharts;

