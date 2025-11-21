// ===== Stock Analysis Page JavaScript =====
// Version: 1.1.0 - Added Fetch Live Data, Download Bhavcopy, and Technical Analysis APIs

// Initialize page
document.addEventListener('DOMContentLoaded', () => {
    setDefaultDates();
});

// Set default dates
function setDefaultDates() {
    const today = new Date().toISOString().split('T')[0];
    
    document.getElementById('btstRecDate').value = today;
    document.getElementById('btstRunDate').value = today;
    document.getElementById('btstDetailedDate').value = today;
    document.getElementById('technicalRunDate').value = today;
    document.getElementById('singleIndicatorDate').value = today;
    document.getElementById('allIndicatorsJsonDate').value = today;
    document.getElementById('bulkDate').value = today;
    document.getElementById('bhavcopyDate').value = today;
}

// BTST Recommendations
window.fetchBTSTRecommendations = async function() {
    const btn = event.target;
    const resultEl = document.getElementById('btstRecResult');
    
    stockAnalyzer.setLoading(btn, true);
    resultEl.textContent = 'Fetching recommendations...';
    
    try {
        const date = document.getElementById('btstRecDate').value;
        const recType = document.getElementById('btstRecType').value;
        
        const params = new URLSearchParams({ recommendation: recType });
        if (date) params.append('date', date);
        
        const data = await stockAnalyzer.fetchAPI(`/api/v1/analysis/btst/recommendations?${params}`);
        
        if (Array.isArray(data)) {
            let formatted = `Found ${data.length} ${recType} recommendations:\n\n`;
            data.forEach((rec, idx) => {
                formatted += `${idx + 1}. ${rec.symbol}\n`;
                formatted += `   Confidence: ${stockAnalyzer.formatPercentage(rec.confidenceScore)}\n`;
                formatted += `   Analysis Date: ${stockAnalyzer.formatDisplayDate(rec.analysisDate)}\n`;
                if (rec.buyReason) formatted += `   Reason: ${rec.buyReason}\n`;
                formatted += '\n';
            });
            resultEl.textContent = formatted;
            stockAnalyzer.showNotification(`Found ${data.length} recommendations`, 'success');
        } else {
            resultEl.textContent = JSON.stringify(data, null, 2);
        }
    } catch (error) {
        resultEl.textContent = `Error: ${error.message}`;
        stockAnalyzer.showNotification('Fetch failed: ' + error.message, 'error');
    } finally {
        stockAnalyzer.setLoading(btn, false);
    }
};

// Run BTST Analysis
window.runBTSTAnalysis = async function() {
    const btn = event.target;
    const resultEl = document.getElementById('btstRunResult');
    
    stockAnalyzer.setLoading(btn, true);
    resultEl.textContent = 'Running BTST analysis... This may take a few moments.';
    
    try {
        const date = document.getElementById('btstRunDate').value;
        const url = date ? `/api/v1/analysis/btst/run?date=${date}` : '/api/v1/analysis/btst/run';
        
        const data = await stockAnalyzer.fetchAPI(url);
        
        if (Array.isArray(data)) {
            resultEl.textContent = `Analysis complete!\n\nProcessed ${data.length} stocks\n\n` + JSON.stringify(data, null, 2);
            stockAnalyzer.showNotification(`Analysis complete for ${data.length} stocks`, 'success');
        } else {
            resultEl.textContent = JSON.stringify(data, null, 2);
        }
    } catch (error) {
        resultEl.textContent = `Error: ${error.message}`;
        stockAnalyzer.showNotification('Analysis failed: ' + error.message, 'error');
    } finally {
        stockAnalyzer.setLoading(btn, false);
    }
};

// Get Detailed BTST Analysis
window.getDetailedBTST = async function() {
    const btn = event.target;
    const resultEl = document.getElementById('btstDetailedResult');
    
    stockAnalyzer.setLoading(btn, true);
    resultEl.textContent = 'Fetching detailed analysis...';
    
    try {
        const symbol = document.getElementById('btstDetailedSymbol').value.toUpperCase();
        const date = document.getElementById('btstDetailedDate').value;
        
        if (!symbol) {
            throw new Error('Please enter a stock symbol');
        }
        
        const url = date 
            ? `/api/v1/analysis/btst/detailed/${symbol}?date=${date}`
            : `/api/v1/analysis/btst/detailed/${symbol}`;
        
        const data = await stockAnalyzer.fetchAPI(url);
        resultEl.textContent = JSON.stringify(data, null, 2);
        stockAnalyzer.showNotification('Details loaded successfully', 'success');
    } catch (error) {
        resultEl.textContent = `Error: ${error.message}`;
        stockAnalyzer.showNotification('Fetch failed: ' + error.message, 'error');
    } finally {
        stockAnalyzer.setLoading(btn, false);
    }
};

// Run Technical Analysis
window.runTechnicalAnalysis = async function() {
    const btn = event.target;
    const resultEl = document.getElementById('technicalRunResult');
    
    stockAnalyzer.setLoading(btn, true);
    resultEl.textContent = 'Calculating technical indicators... This may take several minutes.';
    
    try {
        const date = document.getElementById('technicalRunDate').value;
        const url = date 
            ? `/api/v1/analysis/technical/run?date=${date}`
            : '/api/v1/analysis/technical/run';
        
        const data = await stockAnalyzer.fetchAPI(url);
        
        if (Array.isArray(data)) {
            resultEl.textContent = `Calculation complete!\n\nProcessed ${data.length} stocks\n\n` + JSON.stringify(data.slice(0, 10), null, 2);
            stockAnalyzer.showNotification(`Calculated indicators for ${data.length} stocks`, 'success');
        } else {
            resultEl.textContent = JSON.stringify(data, null, 2);
        }
    } catch (error) {
        resultEl.textContent = `Error: ${error.message}`;
        stockAnalyzer.showNotification('Calculation failed: ' + error.message, 'error');
    } finally {
        stockAnalyzer.setLoading(btn, false);
    }
};

// Get Stock Indicators
window.getStockIndicators = async function() {
    const btn = event.target;
    const resultEl = document.getElementById('singleIndicatorResult');
    
    stockAnalyzer.setLoading(btn, true);
    resultEl.textContent = 'Fetching indicators...';
    
    try {
        const symbol = document.getElementById('singleIndicatorSymbol').value.toUpperCase();
        const date = document.getElementById('singleIndicatorDate').value;
        
        if (!symbol) {
            throw new Error('Please enter a stock symbol');
        }
        
        const url = date 
            ? `/api/v1/analysis/indicators/${symbol}?date=${date}`
            : `/api/v1/analysis/indicators/${symbol}`;
        
        const data = await stockAnalyzer.fetchAPI(url);
        resultEl.textContent = JSON.stringify(data, null, 2);
        stockAnalyzer.showNotification('Indicators loaded successfully', 'success');
    } catch (error) {
        resultEl.textContent = `Error: ${error.message}`;
        stockAnalyzer.showNotification('Fetch failed: ' + error.message, 'error');
    } finally {
        stockAnalyzer.setLoading(btn, false);
    }
};

// Get All Indicators
window.getAllIndicators = async function(format) {
    const btn = event.target;
    const resultEl = document.getElementById('allIndicatorsResult');
    
    stockAnalyzer.setLoading(btn, true);
    
    try {
        const date = document.getElementById('allIndicatorsJsonDate').value;
        
        if (format === 'csv') {
            const url = date 
                ? `/api/v1/analysis/indicators/all?date=${date}`
                : '/api/v1/analysis/indicators/all';
            
            const timestamp = new Date().toISOString().split('T')[0];
            await stockAnalyzer.downloadFromURL(url, `all_indicators_${timestamp}.csv`);
            resultEl.textContent = 'CSV download started...';
        } else {
            const url = date 
                ? `/api/v1/analysis/indicators/all/json?date=${date}`
                : '/api/v1/analysis/indicators/all/json';
            
            resultEl.textContent = 'Fetching indicators...';
            const data = await stockAnalyzer.fetchAPI(url);
            
            if (Array.isArray(data)) {
                resultEl.textContent = `Found ${data.length} indicators\n\n` + JSON.stringify(data.slice(0, 5), null, 2);
                stockAnalyzer.showNotification(`Loaded ${data.length} indicators`, 'success');
            } else {
                resultEl.textContent = JSON.stringify(data, null, 2);
            }
        }
    } catch (error) {
        resultEl.textContent = `Error: ${error.message}`;
        stockAnalyzer.showNotification('Operation failed: ' + error.message, 'error');
    } finally {
        stockAnalyzer.setLoading(btn, false);
    }
};

// Bulk Download
window.bulkDownload = async function() {
    const btn = event.target;
    const resultEl = document.getElementById('bulkResult');
    
    stockAnalyzer.setLoading(btn, true);
    resultEl.textContent = 'Preparing bulk download...';
    
    try {
        const symbols = document.getElementById('bulkSymbols').value;
        const date = document.getElementById('bulkDate').value;
        
        if (!symbols) {
            throw new Error('Please enter stock symbols');
        }
        
        const params = new URLSearchParams({ symbols });
        if (date) params.append('date', date);
        
        const timestamp = new Date().toISOString().split('T')[0];
        await stockAnalyzer.downloadFromURL(
            `/api/v1/analysis/indicators/bulk/download?${params}`,
            `bulk_indicators_${timestamp}.zip`
        );
        
        resultEl.textContent = 'Download started...';
    } catch (error) {
        resultEl.textContent = `Error: ${error.message}`;
        stockAnalyzer.showNotification('Download failed: ' + error.message, 'error');
    } finally {
        stockAnalyzer.setLoading(btn, false);
    }
};

// Reauthenticate
window.reauthenticate = async function() {
    const btn = event.target;
    const resultEl = document.getElementById('reauthResult');
    
    stockAnalyzer.setLoading(btn, true);
    resultEl.textContent = 'Reauthenticating...';
    
    try {
        const totp = document.getElementById('reauthTOTP').value;
        
        if (!totp) {
            throw new Error('Please enter TOTP code');
        }
        
        const data = await stockAnalyzer.fetchAPI(`/reauthenticate?totp=${totp}`);
        resultEl.textContent = typeof data === 'string' ? data : JSON.stringify(data, null, 2);
        stockAnalyzer.showNotification('Reauthentication successful', 'success');
        
        // Clear TOTP after successful auth
        document.getElementById('reauthTOTP').value = '';
    } catch (error) {
        resultEl.textContent = `Error: ${error.message}`;
        stockAnalyzer.showNotification('Reauthentication failed: ' + error.message, 'error');
    } finally {
        stockAnalyzer.setLoading(btn, false);
    }
};

// Fetch Live Data
window.fetchLiveData = async function() {
    const btn = window.event?.target || document.activeElement;
    const resultEl = document.getElementById('fetchLiveResult');
    
    stockAnalyzer.setLoading(btn, true);
    resultEl.textContent = 'Fetching live data from FivePaisa... This may take a few moments.';
    
    try {
        const data = await stockAnalyzer.fetchAPI('/api/v1/analysis/fetch-live-data');
        
        if (typeof data === 'string') {
            resultEl.textContent = data;
            stockAnalyzer.showNotification('Live data fetched successfully', 'success');
        } else {
            resultEl.textContent = JSON.stringify(data, null, 2);
        }
    } catch (error) {
        resultEl.textContent = `Error: ${error.message}`;
        stockAnalyzer.showNotification('Fetch failed: ' + error.message, 'error');
    } finally {
        stockAnalyzer.setLoading(btn, false);
    }
};

// Download Bhavcopy
window.downloadBhavcopy = async function() {
    const btn = window.event?.target || document.activeElement;
    const resultEl = document.getElementById('bhavcopyResult');
    
    stockAnalyzer.setLoading(btn, true);
    resultEl.textContent = 'Downloading and processing bhavcopy... This may take several minutes.';
    
    try {
        const date = document.getElementById('bhavcopyDate').value;
        
        if (!date) {
            throw new Error('Please select a date');
        }
        
        const data = await stockAnalyzer.fetchAPI(`/api/v1/analysis/download-bhavcopy?date=${date}`);
        
        if (typeof data === 'string') {
            resultEl.textContent = data;
            stockAnalyzer.showNotification('Bhavcopy processed successfully', 'success');
        } else {
            resultEl.textContent = JSON.stringify(data, null, 2);
        }
    } catch (error) {
        resultEl.textContent = `Error: ${error.message}`;
        stockAnalyzer.showNotification('Download failed: ' + error.message, 'error');
    } finally {
        stockAnalyzer.setLoading(btn, false);
    }
};

// Log that new functions are loaded
console.log('Stock Analysis: fetchLiveData and downloadBhavcopy functions loaded');

