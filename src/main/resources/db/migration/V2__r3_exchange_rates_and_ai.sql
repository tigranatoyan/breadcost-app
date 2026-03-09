-- ============================================================
-- Flyway V2 — R3 Sprint 1: Exchange rates, AI conversations
-- ============================================================

-- -----------------------------------------------------------
-- 1. exchange_rates — per currency per date (FR-9.7)
-- -----------------------------------------------------------
CREATE TABLE exchange_rates (
    rate_id       VARCHAR(255) PRIMARY KEY,
    tenant_id     VARCHAR(255) NOT NULL,
    base_currency VARCHAR(10)  NOT NULL DEFAULT 'USD',
    currency_code VARCHAR(10)  NOT NULL,
    rate          DOUBLE PRECISION NOT NULL,
    rate_date     DATE         NOT NULL,
    source        VARCHAR(50)  NOT NULL DEFAULT 'MANUAL',
    created_at    TIMESTAMP WITH TIME ZONE,
    updated_at    TIMESTAMP WITH TIME ZONE,
    UNIQUE (tenant_id, base_currency, currency_code, rate_date)
);

CREATE INDEX idx_exchange_rates_tenant_date ON exchange_rates(tenant_id, currency_code, rate_date);

-- -----------------------------------------------------------
-- 2. ai_conversations — WhatsApp AI sessions (FR-12.1)
-- -----------------------------------------------------------
CREATE TABLE ai_conversations (
    conversation_id VARCHAR(255) PRIMARY KEY,
    tenant_id       VARCHAR(255) NOT NULL,
    customer_phone  VARCHAR(100) NOT NULL,
    customer_id     VARCHAR(255),
    channel         VARCHAR(50)  NOT NULL DEFAULT 'WHATSAPP',
    status          VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    escalated       BOOLEAN      NOT NULL DEFAULT FALSE,
    escalation_reason VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE,
    updated_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_ai_conv_tenant ON ai_conversations(tenant_id, status);
CREATE INDEX idx_ai_conv_phone  ON ai_conversations(tenant_id, customer_phone);

-- -----------------------------------------------------------
-- 3. ai_messages — messages within conversations (FR-12.2)
-- -----------------------------------------------------------
CREATE TABLE ai_messages (
    message_id      VARCHAR(255) PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL REFERENCES ai_conversations(conversation_id),
    tenant_id       VARCHAR(255) NOT NULL,
    direction       VARCHAR(10)  NOT NULL,
    content         TEXT         NOT NULL,
    message_type    VARCHAR(50)  NOT NULL DEFAULT 'TEXT',
    parsed_intent   VARCHAR(100),
    confidence      DOUBLE PRECISION,
    created_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_ai_msg_conv ON ai_messages(conversation_id, created_at);

-- -----------------------------------------------------------
-- 4. ai_draft_orders — orders parsed from AI conversations
-- -----------------------------------------------------------
CREATE TABLE ai_draft_orders (
    draft_id        VARCHAR(255) PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL REFERENCES ai_conversations(conversation_id),
    tenant_id       VARCHAR(255) NOT NULL,
    customer_id     VARCHAR(255),
    status          VARCHAR(50)  NOT NULL DEFAULT 'PENDING_CONFIRMATION',
    confirmed_order_id VARCHAR(255),
    upsell_offered  BOOLEAN      NOT NULL DEFAULT FALSE,
    upsell_accepted BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE,
    updated_at      TIMESTAMP WITH TIME ZONE
);

-- -----------------------------------------------------------
-- 5. ai_draft_order_lines — line items in draft orders
-- -----------------------------------------------------------
CREATE TABLE ai_draft_order_lines (
    line_id    VARCHAR(255) PRIMARY KEY,
    draft_id   VARCHAR(255) NOT NULL REFERENCES ai_draft_orders(draft_id),
    tenant_id  VARCHAR(255) NOT NULL,
    product_id VARCHAR(255),
    product_name VARCHAR(255),
    qty        DOUBLE PRECISION NOT NULL DEFAULT 1,
    unit       VARCHAR(50),
    is_upsell  BOOLEAN NOT NULL DEFAULT FALSE
);

-- -----------------------------------------------------------
-- 6. supplier_api_configs — per-supplier API settings (FR-6.4)
-- -----------------------------------------------------------
CREATE TABLE supplier_api_configs (
    config_id     VARCHAR(255) PRIMARY KEY,
    tenant_id     VARCHAR(255) NOT NULL,
    supplier_id   VARCHAR(255) NOT NULL REFERENCES suppliers(supplier_id),
    api_url       VARCHAR(1000),
    api_key_ref   VARCHAR(255),
    format        VARCHAR(50)  NOT NULL DEFAULT 'JSON',
    enabled       BOOLEAN      NOT NULL DEFAULT FALSE,
    last_sent_at  TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE,
    updated_at    TIMESTAMP WITH TIME ZONE,
    UNIQUE (tenant_id, supplier_id)
);
