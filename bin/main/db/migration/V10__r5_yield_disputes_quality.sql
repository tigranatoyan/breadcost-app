-- V10: R5 schema additions — yield tracking, invoice disputes, quality predictions, supplier preferred flag

-- G-9: Yield tracking on work orders
ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS actual_yield DOUBLE PRECISION;
ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS waste_qty DOUBLE PRECISION;
ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS quality_score VARCHAR(255);
ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS quality_notes TEXT;
ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS yield_variance_pct DOUBLE PRECISION;

-- G-4: Invoice dispute workflow
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS dispute_reason TEXT;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS disputed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS disputed_by VARCHAR(255);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS resolution_notes TEXT;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS credit_note_amount NUMERIC(19, 2);

-- G-10: Supplier preferred flag
ALTER TABLE supplier_catalog_items ADD COLUMN IF NOT EXISTS preferred BOOLEAN DEFAULT FALSE;

-- D3.4: AI quality predictions
CREATE TABLE IF NOT EXISTS ai_quality_predictions (
    prediction_id   VARCHAR(255) PRIMARY KEY,
    tenant_id       VARCHAR(255) NOT NULL,
    product_id      VARCHAR(255) NOT NULL,
    product_name    VARCHAR(255),
    recipe_id       VARCHAR(255),
    risk_level      VARCHAR(255) NOT NULL,
    predicted_yield_pct     DOUBLE PRECISION,
    historical_avg_yield_pct DOUBLE PRECISION,
    risk_factors    TEXT,
    recommendation  TEXT,
    confidence      DOUBLE PRECISION,
    status          VARCHAR(255) DEFAULT 'ACTIVE',
    created_at      TIMESTAMP WITH TIME ZONE
);
