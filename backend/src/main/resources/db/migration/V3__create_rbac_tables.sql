-- V3 Role-Based Access Control Schema
-- CodePilot AI RBAC Domain Migration

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert Default System Roles
INSERT INTO roles (name, description) VALUES
    ('ROLE_ADMIN', 'System Administrator with full access to tenant, configuration, and user management'),
    ('ROLE_DEVELOPER', 'Core Developer with access to repository imports, AI chat, code reviews, and SQL optimization'),
    ('ROLE_VIEWER', 'Read-only stakeholder access to documentation and search');
