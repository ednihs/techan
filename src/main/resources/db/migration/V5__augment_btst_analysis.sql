ALTER TABLE btst_analysis
    ADD COLUMN IF NOT EXISTS liquidity_risk_level ENUM('LOW','MEDIUM','HIGH'),
    ADD COLUMN IF NOT EXISTS liquidity_risk_factors TEXT,
    ADD COLUMN IF NOT EXISTS gap_risk_level ENUM('LOW','MEDIUM','HIGH'),
    ADD COLUMN IF NOT EXISTS gap_risk_factors TEXT,
    ADD COLUMN IF NOT EXISTS catalyst_score INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS catalyst_details TEXT,
    ADD COLUMN IF NOT EXISTS automated_risk_assessment BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS position_size_percent DECIMAL(4,2),
    ADD COLUMN IF NOT EXISTS risk_reward_ratio_t1 DECIMAL(4,2),
    ADD COLUMN IF NOT EXISTS risk_reward_ratio_t2 DECIMAL(4,2);

CREATE TABLE IF NOT EXISTS automated_research_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    research_date DATE NOT NULL,
    catalyst_score INT,
    catalyst_details TEXT,
    research_source VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_symbol_research_date (symbol, research_date),
    INDEX idx_catalyst_score (catalyst_score DESC),
    CONSTRAINT fk_research_symbol FOREIGN KEY (symbol) REFERENCES stocks(symbol) ON DELETE CASCADE
);
