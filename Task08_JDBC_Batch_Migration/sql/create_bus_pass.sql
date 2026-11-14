CREATE DATABASE IF NOT EXISTS transport_db;
USE transport_db;

CREATE TABLE IF NOT EXISTS bus_pass (
    pass_id VARCHAR(32) PRIMARY KEY,
    reg_no VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    route_no INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL
);
