-- BC-3001: Notification templates table
CREATE TABLE notification_templates (
    template_id       VARCHAR(36) PRIMARY KEY,
    tenant_id         VARCHAR(64) NOT NULL,
    type              VARCHAR(64) NOT NULL,
    channel           VARCHAR(32) NOT NULL,
    subject           VARCHAR(255),
    body_template     VARCHAR(4000),
    active            BOOLEAN DEFAULT TRUE,
    created_at_epoch_ms BIGINT,
    updated_at_epoch_ms BIGINT,
    CONSTRAINT uq_template_tenant_type_channel UNIQUE (tenant_id, type, channel)
);

CREATE INDEX idx_notif_template_tenant ON notification_templates (tenant_id);
CREATE INDEX idx_notif_template_type   ON notification_templates (tenant_id, type);
