-- Create technical_indicators table
CREATE TABLE technical_indicators (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    calculation_date DATE NOT NULL,
    
    -- Price-based indicators
    rsi_14 DOUBLE,
    ema_9 DOUBLE,
    ema_21 DOUBLE,
    sma_20 DOUBLE,
    atr_14 DOUBLE,
    vwap DOUBLE,
    
    -- Volume-based indicators
    volume_sma_20 BIGINT,
    volume_ratio DOUBLE,
    
    -- Custom strength indicators
    price_strength DOUBLE,
    volume_strength DOUBLE,
    delivery_strength DOUBLE,
    
    -- Momentum indicators
    macd DOUBLE,
    macd_signal DOUBLE,
    macd_histogram DOUBLE,
    
    -- Volatility indicators
    bollinger_upper DOUBLE,
    bollinger_lower DOUBLE,
    bollinger_width DOUBLE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_symbol_calc_date (symbol, calculation_date),
    INDEX idx_calculation_date (calculation_date),
    INDEX idx_rsi_14 (rsi_14),
    INDEX idx_volume_ratio (volume_ratio DESC),
    
    FOREIGN KEY (symbol) REFERENCES stocks(symbol) ON DELETE CASCADE
);
