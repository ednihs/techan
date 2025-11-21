CREATE TABLE IF NOT EXISTS crude_ohlcv_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    timeframe ENUM('4H', '1H', '15M') NOT NULL,
    symbol VARCHAR(10) DEFAULT 'BRN' NOT NULL,
    open DECIMAL(10, 4) NOT NULL,
    high DECIMAL(10, 4) NOT NULL,
    low DECIMAL(10, 4) NOT NULL,
    close DECIMAL(10, 4) NOT NULL,
    volume BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes for performance
    UNIQUE KEY unique_candle (symbol, timeframe, timestamp),
    INDEX idx_timeframe_timestamp (timeframe, timestamp DESC),
    INDEX idx_symbol_timeframe (symbol, timeframe)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OHLCV candle data for crude oil analysis';

