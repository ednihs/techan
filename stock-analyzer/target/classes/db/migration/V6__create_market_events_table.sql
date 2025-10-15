-- Create market_events table
CREATE TABLE market_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    event_date DATE NOT NULL,
    event_type VARCHAR(100),
    event_description TEXT,
    impact_score DOUBLE,
    source VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_symbol_event_date (symbol, event_date),
    INDEX idx_event_type (event_type),
    
    FOREIGN KEY (symbol) REFERENCES stocks(symbol) ON DELETE CASCADE
);
