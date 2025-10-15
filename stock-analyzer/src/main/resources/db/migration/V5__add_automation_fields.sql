-- Add automated risk assessment fields to btst_analysis table
ALTER TABLE btst_analysis ADD COLUMN liquidity_risk_level VARCHAR(255);
ALTER TABLE btst_analysis ADD COLUMN liquidity_risk_factors TEXT;
ALTER TABLE btst_analysis ADD COLUMN gap_risk_level VARCHAR(255);
ALTER TABLE btst_analysis ADD COLUMN gap_risk_factors TEXT;
ALTER TABLE btst_analysis ADD COLUMN catalyst_score INT DEFAULT 0;
ALTER TABLE btst_analysis ADD COLUMN catalyst_details TEXT;
ALTER TABLE btst_analysis ADD COLUMN automated_risk_assessment BOOLEAN DEFAULT TRUE;
ALTER TABLE btst_analysis ADD COLUMN position_size_percent DOUBLE;
ALTER TABLE btst_analysis ADD COLUMN risk_reward_ratio_t1 DOUBLE;
ALTER TABLE btst_analysis ADD COLUMN risk_reward_ratio_t2 DOUBLE;

-- Create automated_research_results table
CREATE TABLE automated_research_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    research_date DATE NOT NULL,
    catalyst_type VARCHAR(100),
    catalyst_description TEXT,
    catalyst_score INT DEFAULT 0,
    news_sentiment_score DOUBLE,
    research_source VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_symbol_research_date (symbol, research_date),
    INDEX idx_catalyst_score (catalyst_score DESC),
    
    FOREIGN KEY (symbol) REFERENCES stocks(symbol) ON DELETE CASCADE
);
