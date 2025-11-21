CREATE TABLE IF NOT EXISTS crude_technical_indicators (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ohlcv_id BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    timeframe ENUM('4H', '1H', '15M') NOT NULL,
    
    -- Price-based indicators
    rsi_14 DECIMAL(6, 2),
    macd_line DECIMAL(8, 4),
    macd_signal DECIMAL(8, 4),
    macd_histogram DECIMAL(8, 4),
    macd_crossover_signal ENUM('bullish', 'bearish', 'neutral'),
    bb_upper DECIMAL(10, 4),
    bb_middle DECIMAL(10, 4),
    bb_lower DECIMAL(10, 4),
    bb_width DECIMAL(8, 4),
    atr_14 DECIMAL(8, 4),
    sma_20 DECIMAL(10, 4),
    sma_50 DECIMAL(10, 4),
    sma_100 DECIMAL(10, 4),
    sma_200 DECIMAL(10, 4),
    ema_12 DECIMAL(10, 4),
    ema_26 DECIMAL(10, 4),
    
    -- Price vs SMA percentages
    price_vs_sma20 VARCHAR(10),
    price_vs_sma50 VARCHAR(10),
    price_vs_sma100 VARCHAR(10),
    price_vs_sma200 VARCHAR(10),
    
    -- Support/Resistance levels
    highest_20 DECIMAL(10, 4),
    lowest_20 DECIMAL(10, 4),
    support_1 DECIMAL(10, 4),
    support_2 DECIMAL(10, 4),
    resistance_1 DECIMAL(10, 4),
    resistance_2 DECIMAL(10, 4),
    
    -- Volume-based indicators
    obv BIGINT COMMENT 'On-Balance Volume (cumulative)',
    obv_ema DECIMAL(12, 2) COMMENT 'OBV 10-period EMA for smoothing',
    vwap DECIMAL(10, 4) COMMENT 'Volume Weighted Average Price',
    volume_sma_20 BIGINT COMMENT '20-period volume moving average',
    volume_ratio DECIMAL(6, 2) COMMENT 'Current volume / SMA volume (%)',
    price_volume_trend DECIMAL(12, 2) COMMENT 'PVT cumulative indicator',
    volume_rate_of_change DECIMAL(8, 2) COMMENT 'Volume % change from previous period',
    
    -- Metadata
    data_quality_flag ENUM('Fresh', 'Recent', 'Stale') NOT NULL,
    day_type ENUM('Regular', 'EIA_Release', 'Fed_Event', 'OPEC_Update', 'Geopolitical') DEFAULT 'Regular',
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key and indexes
    FOREIGN KEY (ohlcv_id) REFERENCES crude_ohlcv_data(id) ON DELETE CASCADE,
    UNIQUE KEY unique_indicator (ohlcv_id),
    INDEX idx_timeframe_timestamp (timeframe, timestamp DESC),
    INDEX idx_data_quality (data_quality_flag, timestamp DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Technical indicators for crude oil analysis with volume-based indicators';
