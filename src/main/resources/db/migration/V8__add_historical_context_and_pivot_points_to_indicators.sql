ALTER TABLE technical_indicators
    ADD COLUMN week_52_high FLOAT,
    ADD COLUMN week_52_low FLOAT,
    ADD COLUMN days_since_52w_high INT,
    ADD COLUMN intraday_fade_pct FLOAT,
    ADD COLUMN close_velocity_5d FLOAT,
    ADD COLUMN prev_day_open FLOAT,
    ADD COLUMN prev_day_high FLOAT,
    ADD COLUMN prev_day_low FLOAT,
    ADD COLUMN prev_day_close FLOAT,
    ADD COLUMN rsi_14_prev_day FLOAT,
    ADD COLUMN macd_histogram_prev_day FLOAT,
    ADD COLUMN pivot_point FLOAT,
    ADD COLUMN resistance_1 FLOAT,
    ADD COLUMN resistance_2 FLOAT,
    ADD COLUMN support_1 FLOAT,
    ADD COLUMN support_2 FLOAT,
    ADD COLUMN pct_from_pivot FLOAT,
    ADD COLUMN pct_to_resistance_1 FLOAT,
    ADD COLUMN pct_to_resistance_2 FLOAT,
    ADD COLUMN pct_to_support_1 FLOAT,
    ADD COLUMN pct_to_support_2 FLOAT,
    ADD COLUMN price_position_stage VARCHAR(20),
    ADD COLUMN momentum_direction VARCHAR(20),
    ADD COLUMN data_completeness VARCHAR(20);





