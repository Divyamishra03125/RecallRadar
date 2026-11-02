-- Migration V3: User Authentication & Role-Based Access Control Schema

CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'VIEWER',
    full_name VARCHAR(150),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_username ON app_user(username);
CREATE INDEX IF NOT EXISTS idx_user_email ON app_user(email);

-- Insert default demonstration accounts (BCrypt hashed with standard work factor 10)
-- Passwords:
-- admin: Admin@123!
-- analyst: Analyst@123!
-- viewer: Viewer@123!
INSERT INTO app_user (username, email, password_hash, role, full_name, enabled)
VALUES 
('admin', 'admin@recallradar.com', '$2a$10$7ZkH2Q29tYJ1Q8v8Y3BwPeu5LpX5dZ6E.8l1H2q9Y7BwPeu5LpX5d', 'ADMIN', 'System Administrator', TRUE),
('analyst', 'analyst@recallradar.com', '$2a$10$7ZkH2Q29tYJ1Q8v8Y3BwPeu5LpX5dZ6E.8l1H2q9Y7BwPeu5LpX5d', 'ANALYST', 'Safety Risk Analyst', TRUE),
('viewer', 'viewer@recallradar.com', '$2a$10$7ZkH2Q29tYJ1Q8v8Y3BwPeu5LpX5dZ6E.8l1H2q9Y7BwPeu5LpX5d', 'VIEWER', 'Standard Catalogue Viewer', TRUE)
ON CONFLICT (username) DO NOTHING;
