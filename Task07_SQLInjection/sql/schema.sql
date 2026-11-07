CREATE DATABASE IF NOT EXISTS csa09_a5_test;
USE csa09_a5_test;

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    pwd VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS bus_pass (
    pass_id VARCHAR(32) PRIMARY KEY,
    reg_no VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    route_no INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL
);

INSERT INTO users (id, pwd) VALUES ('alice', 'correct-password')
ON DUPLICATE KEY UPDATE pwd = VALUES(pwd);
