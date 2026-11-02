-- Migration V6: User Account Fields, Password Hash Synchronization, and Admin Audit Log

-- 1. Extend app_user with updated_at and last_login_at timestamps
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;

-- 2. Synchronize seeded demonstration accounts with verified BCrypt hashes
-- Passwords:
-- admin:   Admin@123!
-- analyst: Analyst@123!
-- viewer:  Viewer@123!
UPDATE app_user 
SET password_hash = '$2a$10$m6JNLhxVGr46iXIyUgnfueftVwnu4EHwo69XmEz7T/VEeDidiKAs2',
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'admin';

UPDATE app_user 
SET password_hash = '$2a$10$DgA1jd92ai2lDWza.iOG6uGMa.d4BbkcWIDQblOv4gmhjEGe68FOq',
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'analyst';

UPDATE app_user 
SET password_hash = '$2a$10$zBHIesH93i8oiVCC9kWbOeKEDQ6cfWzojLLULx27JK.F1jACyqtO.',
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'viewer';

-- 3. Create user_audit_log for administrative tracking of role and account status changes
CREATE TABLE IF NOT EXISTS user_audit_log (
    id BIGSERIAL PRIMARY KEY,
    target_user_id BIGINT REFERENCES app_user(id) ON DELETE CASCADE,
    target_username VARCHAR(100) NOT NULL,
    performed_by VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    previous_value VARCHAR(100),
    new_value VARCHAR(100),
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_target_user ON user_audit_log(target_user_id);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON user_audit_log(created_at);
