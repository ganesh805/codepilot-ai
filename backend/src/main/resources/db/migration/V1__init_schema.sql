-- V1 Initial Schema Setup
-- CodePilot AI Engine Baseline Database Migration

CREATE TABLE IF NOT EXISTS system_metadata (
    id VARCHAR(64) PRIMARY KEY,
    property_key VARCHAR(100) NOT NULL UNIQUE,
    property_value VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO system_metadata (id, property_key, property_value) 
VALUES ('sys-01', 'schema_version', '1.0.0'),
       ('sys-02', 'app_name', 'CodePilot AI Developer Assistant');
