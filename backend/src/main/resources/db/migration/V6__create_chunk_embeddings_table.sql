-- V6 Code Chunk Embeddings & Qdrant Vector Persistence Migration
-- CodePilot AI RAG Vector Pipeline

CREATE TABLE IF NOT EXISTS chunk_embeddings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    chunk_id BIGINT NOT NULL,
    repository_id BIGINT NOT NULL,
    qdrant_point_id VARCHAR(100) NOT NULL,
    vector_dimension INT NOT NULL DEFAULT 768,
    status VARCHAR(30) NOT NULL DEFAULT 'INDEXED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chunk_id) REFERENCES code_chunks(id) ON DELETE CASCADE,
    FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE,
    INDEX idx_embedding_repo (repository_id),
    INDEX idx_embedding_chunk (chunk_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
