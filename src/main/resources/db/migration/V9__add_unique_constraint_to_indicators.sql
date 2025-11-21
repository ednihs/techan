ALTER TABLE technical_indicators
ADD CONSTRAINT uc_symbol_calculation_date UNIQUE (symbol, calculation_date);






