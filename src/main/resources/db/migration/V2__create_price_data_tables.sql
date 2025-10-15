CREATE TABLE IF NOT EXISTS price_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,
    open_price DECIMAL(12,2),
    high_price DECIMAL(12,2),
    low_price DECIMAL(12,2),
    close_price DECIMAL(12,2),
    prev_close_price DECIMAL(12,2),
    volume BIGINT,
    value_traded BIGINT,
    no_of_trades INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_symbol_trade_date (symbol, trade_date),
    INDEX idx_trade_date_value (trade_date, value_traded DESC),
    INDEX idx_symbol_date_desc (symbol, trade_date DESC),
    CONSTRAINT fk_price_data_symbol FOREIGN KEY (symbol) REFERENCES stocks(symbol) ON DELETE CASCADE
);
