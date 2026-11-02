-- Migration V2: Add association_status, severity audit trail, and performance indexes

ALTER TABLE recall ADD COLUMN IF NOT EXISTS association_status VARCHAR(30) NOT NULL DEFAULT 'UNLINKED';

-- Populate initial association_status based on existing product associations
UPDATE recall 
SET association_status = 'LINKED' 
WHERE product_id IS NOT NULL;

-- Severity review audit trail table
CREATE TABLE IF NOT EXISTS recall_severity_audit (
    id BIGSERIAL PRIMARY KEY,
    recall_id BIGINT NOT NULL REFERENCES recall(id) ON DELETE CASCADE,
    old_severity VARCHAR(30) NOT NULL,
    new_severity VARCHAR(30) NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_severity_audit_recall_id ON recall_severity_audit(recall_id);
CREATE INDEX IF NOT EXISTS idx_product_brand ON product(brand);
CREATE INDEX IF NOT EXISTS idx_product_model ON product(model_number);
CREATE INDEX IF NOT EXISTS idx_product_batch ON product(batch_number);
CREATE INDEX IF NOT EXISTS idx_recall_association_status ON recall(association_status);
