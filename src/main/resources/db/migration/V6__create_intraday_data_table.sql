CREATE TABLE IF NOT EXISTS intraday_data (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(50) NOT NULL,
    trade_date DATE NOT NULL,
    trade_time TIME NOT NULL,
    open_price NUMERIC(12,2),
    high_price NUMERIC(12,2),
    low_price NUMERIC(12,2),
    close_price NUMERIC(12,2),
    volume BIGINT,
    bid NUMERIC(12,2),
    ask NUMERIC(12,2),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_intraday_symbol_date_time
    ON intraday_data (symbol, trade_date, trade_time);

CREATE INDEX IF NOT EXISTS idx_intraday_trade_date
    ON intraday_data (trade_date);
