-- V12 System Analytics & Operational Control Panel Migration
-- CodePilot AI Analytics Module

CREATE TABLE IF NOT EXISTS system_analytics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    metric_key VARCHAR(100) NOT NULL,
    metric_value BIGINT NOT NULL DEFAULT 0,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_analytics_key (metric_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
