-- V11 SQL Query Optimizer & EXPLAIN Plan Analyzer Migration
-- CodePilot AI SQL Optimizer Module

CREATE TABLE IF NOT EXISTS sql_optimizations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    repository_id BIGINT,
    raw_sql LONGTEXT NOT NULL,
    optimized_sql LONGTEXT NOT NULL,
    indexing_ddl LONGTEXT NOT NULL,
    performance_gain_pct INT NOT NULL DEFAULT 50,
    analysis_summary LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE SET NULL,
    INDEX idx_sql_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
