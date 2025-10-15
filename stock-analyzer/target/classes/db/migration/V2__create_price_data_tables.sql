-- Create price_data table with optimized indexes
CREATE TABLE price_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,
    open_price DOUBLE NOT NULL,
    high_price DOUBLE NOT NULL,
    low_price DOUBLE NOT NULL,
    close_price DOUBLE NOT NULL,
    prev_close DOUBLE NOT NULL,
    volume BIGINT NOT NULL,
    value_traded BIGINT NOT NULL,
    no_of_trades INTEGER NOT NULL,
    delivery_percentage DOUBLE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_symbol_trade_date (symbol, trade_date),
    INDEX idx_trade_date (trade_date),
    INDEX idx_volume (volume DESC),
    INDEX idx_value_traded (value_traded DESC),
    INDEX idx_symbol_date_desc (symbol, trade_date DESC),
    
    FOREIGN KEY (symbol) REFERENCES stocks(symbol) ON DELETE CASCADE
);

-- Create intraday_data table for more granular analysis (optional)
CREATE TABLE intraday_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,
    trade_time TIME NOT NULL,
    price DOUBLE NOT NULL,
    volume INTEGER NOT NULL,
    value_traded INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_symbol_datetime (symbol, trade_date, trade_time),
    INDEX idx_trade_date (trade_date),
    
    FOREIGN KEY (symbol) REFERENCES stocks(symbol) ON DELETE CASCADE
);
