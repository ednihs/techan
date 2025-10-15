-- Initial database setup
CREATE DATABASE IF NOT EXISTS stock_analyzer;
USE stock_analyzer;

-- Create user if not exists
CREATE USER IF NOT EXISTS 'stock_user'@'%' IDENTIFIED BY 'stock_password';
GRANT ALL PRIVILEGES ON stock_analyzer.* TO 'stock_user'@'%';
FLUSH PRIVILEGES;

-- Set MySQL configuration for stock data optimization
SET GLOBAL innodb_buffer_pool_size = 1073741824; -- 1GB
SET GLOBAL max_connections = 500;
SET GLOBAL query_cache_size = 268435456; -- 256MB
