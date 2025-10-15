CREATE DATABASE IF NOT EXISTS stock_analyzer;
USE stock_analyzer;
CREATE USER IF NOT EXISTS 'stock_user'@'%' IDENTIFIED BY 'stock_password';
GRANT ALL PRIVILEGES ON stock_analyzer.* TO 'stock_user'@'%';
FLUSH PRIVILEGES;
