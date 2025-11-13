-- This script deletes duplicate technical indicators, keeping the most recent entry for each symbol and date.
-- It's designed for MySQL. Please backup your data before running.

CREATE TEMPORARY TABLE keepers AS
SELECT MAX(id) as id
FROM technical_indicators
GROUP BY symbol, calculation_date;

DELETE FROM technical_indicators
WHERE id NOT IN (SELECT id FROM keepers);

DROP TEMPORARY TABLE keepers;


