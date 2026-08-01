-- V4 Repository Management Schema Definition
-- CodePilot AI Repository Import Engine Migration

CREATE TABLE IF NOT EXISTS repositories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    owner VARCHAR(100),
    git_url VARCHAR(255),
    import_type VARCHAR(20) NOT NULL,
    storage_path VARCHAR(255) NOT NULL,
    default_branch VARCHAR(50) DEFAULT 'main',
    file_count INT DEFAULT 0,
    total_size_bytes BIGINT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'READY',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_repositories_user (user_id),
    INDEX idx_repositories_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
