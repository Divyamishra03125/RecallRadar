-- Migration V7: Indexes for multi-user role and status query optimization

CREATE INDEX IF NOT EXISTS idx_user_role ON app_user(role);
CREATE INDEX IF NOT EXISTS idx_user_enabled ON app_user(enabled);
CREATE INDEX IF NOT EXISTS idx_user_role_enabled ON app_user(role, enabled);
