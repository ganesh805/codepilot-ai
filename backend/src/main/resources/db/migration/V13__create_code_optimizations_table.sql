-- V13 AI Code Optimizer & Algorithm Engine Migration
-- CodePilot AI Code Optimizer Module

CREATE TABLE IF NOT EXISTS code_optimizations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    repository_id BIGINT,
    language VARCHAR(50) NOT NULL,
    optimization_level VARCHAR(50) NOT NULL,
    raw_code LONGTEXT NOT NULL,
    optimized_code LONGTEXT NOT NULL,
    time_complexity_before VARCHAR(100),
    time_complexity_after VARCHAR(100),
    space_complexity_before VARCHAR(100),
    space_complexity_after VARCHAR(100),
    full_report_markdown LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE SET NULL,
    INDEX idx_code_opt_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
