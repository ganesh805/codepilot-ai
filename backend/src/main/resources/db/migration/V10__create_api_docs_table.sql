-- V10 REST API Documentation Generator & OpenAPI Spec Migration
-- CodePilot AI API Documentation Module

CREATE TABLE IF NOT EXISTS api_docs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    repository_id BIGINT NOT NULL,
    total_endpoints INT NOT NULL DEFAULT 0,
    markdown_spec LONGTEXT NOT NULL,
    openapi_json LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE,
    INDEX idx_doc_repo (repository_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
