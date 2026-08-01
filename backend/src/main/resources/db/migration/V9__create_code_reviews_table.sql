-- V9 AI Code Reviewer Engine & Security Scanner Migration
-- CodePilot AI Code Reviewer Module

CREATE TABLE IF NOT EXISTS code_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    repository_id BIGINT,
    pr_title VARCHAR(200) NOT NULL,
    git_diff LONGTEXT NOT NULL,
    quality_score INT NOT NULL DEFAULT 100,
    security_issues_count INT NOT NULL DEFAULT 0,
    summary LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE SET NULL,
    INDEX idx_review_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
