ALTER TABLE technical_indicators
ADD COLUMN macd DOUBLE,
ADD COLUMN macd_signal DOUBLE,
ADD COLUMN macd_histogram DOUBLE,
ADD COLUMN bollinger_upper DOUBLE,
ADD COLUMN bollinger_lower DOUBLE,
ADD COLUMN bollinger_width DOUBLE;
