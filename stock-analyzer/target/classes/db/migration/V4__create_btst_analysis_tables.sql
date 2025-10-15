-- Create btst_analysis table
CREATE TABLE btst_analysis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    analysis_date DATE NOT NULL,
    
    -- Day-1 BTST characteristics
    had_late_surge BOOLEAN DEFAULT FALSE,
    late_session_volume_ratio DOUBLE,
    breakout_level DOUBLE,
    had_catalyst BOOLEAN DEFAULT FALSE,
    catalyst_type VARCHAR(50),
    
    -- Day-2 Weak hands indicators
    gap_percentage DOUBLE,
    shows_absorption BOOLEAN DEFAULT FALSE,
    average_trade_size DOUBLE,
    retail_intensity DOUBLE,
    vwap_reclaimed BOOLEAN DEFAULT FALSE,
    cumulative_delta DOUBLE,
    
    -- Technical setup
    pullback_depth DOUBLE,
    supply_exhaustion BOOLEAN DEFAULT FALSE,
    strength_score DOUBLE,
    
    -- Final recommendation
    recommendation VARCHAR(255) NOT NULL,
    confidence_score DOUBLE,
    entry_price DOUBLE,
    target_price DOUBLE,
    stop_loss DOUBLE,
    
    -- Risk metrics
    risk_reward_ratio DOUBLE,
    position_size_percentage DOUBLE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_symbol_analysis_date (symbol, analysis_date),
    INDEX idx_analysis_date (analysis_date),
    INDEX idx_recommendation (recommendation),
    INDEX idx_confidence_score (confidence_score DESC),
    INDEX idx_strength_score (strength_score DESC),
    
    FOREIGN KEY (symbol) REFERENCES stocks(symbol) ON DELETE CASCADE
);

-- Create analysis_performance table to track recommendation success
CREATE TABLE analysis_performance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    analysis_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    recommendation_date DATE NOT NULL,
    entry_price DOUBLE,
    exit_price DOUBLE,
    exit_date DATE,
    exit_reason VARCHAR(255),
    actual_return DOUBLE,
    predicted_return DOUBLE,
    success BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_symbol_rec_date (symbol, recommendation_date),
    INDEX idx_success (success),
    INDEX idx_exit_reason (exit_reason),
    
    FOREIGN KEY (analysis_id) REFERENCES btst_analysis(id) ON DELETE CASCADE,
    FOREIGN KEY (symbol) REFERENCES stocks(symbol) ON DELETE CASCADE
);
