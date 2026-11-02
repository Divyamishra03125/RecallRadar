CREATE TABLE product (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    brand VARCHAR(150),
    category VARCHAR(100),
    model_number VARCHAR(100),
    batch_number VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE recall (
    id BIGSERIAL PRIMARY KEY,
    source_record_id VARCHAR(150) UNIQUE,
    product_id BIGINT REFERENCES product(id),
    title VARCHAR(250) NOT NULL,
    description TEXT,
    reason TEXT,
    severity VARCHAR(30) NOT NULL,
    recall_date DATE,
    source_url TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_recall_date ON recall(recall_date);
CREATE INDEX idx_recall_severity ON recall(severity);
CREATE INDEX idx_recall_product_id ON recall(product_id);
CREATE INDEX idx_product_name ON product(name);