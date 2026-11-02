-- Migration V8: Analyst Access Requests, Account Status, and Approval Metadata

ALTER TABLE app_user ADD COLUMN IF NOT EXISTS account_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS organization VARCHAR(150);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS department VARCHAR(150);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS job_title VARCHAR(150);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS approved_by VARCHAR(100);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500);

-- Safely synchronize existing accounts
UPDATE app_user SET account_status = 'DISABLED' WHERE enabled = FALSE;
UPDATE app_user SET account_status = 'ACTIVE' WHERE enabled = TRUE AND (account_status IS NULL OR account_status = '' OR account_status = 'ACTIVE');

CREATE INDEX IF NOT EXISTS idx_user_account_status ON app_user(account_status);
CREATE INDEX IF NOT EXISTS idx_user_role_status ON app_user(role, account_status);
