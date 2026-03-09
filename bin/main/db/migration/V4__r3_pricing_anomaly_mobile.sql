-- V4: R3-S3 — AI pricing, anomaly alerts, mobile push notifications
-- Stories: BC-2001 (FR-12.5), BC-2002 (FR-12.6), BC-2301 (FR-2.1 mobile)

-- ── BC-2001: AI pricing adjustment suggestions ──────────────────────────────

CREATE TABLE ai_pricing_suggestions (
    suggestion_id   VARCHAR(64)    PRIMARY KEY,
    tenant_id       VARCHAR(64)    NOT NULL,
    product_id      VARCHAR(64),
    product_name    VARCHAR(255),
    customer_id     VARCHAR(64),
    customer_name   VARCHAR(255),
    current_price   DECIMAL(18,4),
    suggested_price DECIMAL(18,4),
    change_pct      DECIMAL(8,4),
    reason          VARCHAR(2000),
    status          VARCHAR(32)    NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── BC-2002: AI report anomaly alerts ────────────────────────────────────────

CREATE TABLE ai_anomaly_alerts (
    alert_id        VARCHAR(64)    PRIMARY KEY,
    tenant_id       VARCHAR(64)    NOT NULL,
    alert_type      VARCHAR(64)    NOT NULL,
    severity        VARCHAR(32)    NOT NULL DEFAULT 'WARNING',
    metric_name     VARCHAR(255),
    expected_value  DECIMAL(18,4),
    actual_value    DECIMAL(18,4),
    deviation_pct   DECIMAL(8,4),
    explanation     VARCHAR(2000),
    suggested_action VARCHAR(2000),
    report_context  VARCHAR(255),
    status          VARCHAR(32)    NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── BC-2301: Mobile customer app — push notification support ─────────────────

CREATE TABLE mobile_device_registrations (
    registration_id VARCHAR(64)    PRIMARY KEY,
    tenant_id       VARCHAR(64)    NOT NULL,
    customer_id     VARCHAR(64)    NOT NULL,
    device_token    VARCHAR(512)   NOT NULL,
    platform        VARCHAR(32)    NOT NULL,
    device_name     VARCHAR(255),
    active          BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE push_notifications (
    notification_id VARCHAR(64)    PRIMARY KEY,
    tenant_id       VARCHAR(64)    NOT NULL,
    customer_id     VARCHAR(64)    NOT NULL,
    title           VARCHAR(255)   NOT NULL,
    body            VARCHAR(2000),
    notification_type VARCHAR(64)  NOT NULL,
    reference_id    VARCHAR(64),
    status          VARCHAR(32)    NOT NULL DEFAULT 'PENDING',
    sent_at         TIMESTAMP,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
