-- V8 Application Log File Analyzer & Severity Metrics Migration
-- CodePilot AI Log Analyzer Module

CREATE TABLE IF NOT EXISTS log_analyses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(150) NOT NULL,
    total_lines INT NOT NULL DEFAULT 0,
    error_count INT NOT NULL DEFAULT 0,
    warn_count INT NOT NULL DEFAULT 0,
    info_count INT NOT NULL DEFAULT 0,
    summary LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_log_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
