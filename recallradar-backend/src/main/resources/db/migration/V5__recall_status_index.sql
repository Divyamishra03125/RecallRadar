-- V5__recall_status_index.sql
-- Optimizes queries filtering recalls by operational status (ACTIVE, INACTIVE, RESOLVED)

CREATE INDEX IF NOT EXISTS idx_recall_status ON recall(status);
