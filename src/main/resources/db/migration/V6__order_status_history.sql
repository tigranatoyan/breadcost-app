-- V6: R4-S3 — Order status history timeline
CREATE TABLE order_status_history (
    id                  VARCHAR(64)  PRIMARY KEY,
    order_id            VARCHAR(64)  NOT NULL,
    tenant_id           VARCHAR(64)  NOT NULL,
    status              VARCHAR(32)  NOT NULL,
    description         VARCHAR(500),
    timestamp_epoch_ms  BIGINT       NOT NULL
);
CREATE INDEX idx_osh_order ON order_status_history(order_id);
CREATE INDEX idx_osh_tenant ON order_status_history(tenant_id);
