-- V7 Exception Analysis & Stack Trace Debugger Migration
-- CodePilot AI Debugger Module

CREATE TABLE IF NOT EXISTS exception_analyses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    repository_id BIGINT,
    exception_type VARCHAR(200) NOT NULL,
    error_message TEXT NOT NULL,
    stack_trace LONGTEXT NOT NULL,
    root_cause LONGTEXT NOT NULL,
    suggested_fix LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE SET NULL,
    INDEX idx_exception_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
