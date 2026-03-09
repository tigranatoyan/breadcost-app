-- ============================================================
-- Flyway V3 — R3 Sprint 2: AI Suggestions, Driver Mobile
-- ============================================================

-- -----------------------------------------------------------
-- 1. ai_replenishment_hints — per-item restock hints (FR-12.3)
-- -----------------------------------------------------------
CREATE TABLE ai_replenishment_hints (
    hint_id       VARCHAR(255) PRIMARY KEY,
    tenant_id     VARCHAR(255) NOT NULL,
    item_id       VARCHAR(255) NOT NULL,
    item_name     VARCHAR(255),
    current_qty   DOUBLE PRECISION NOT NULL DEFAULT 0,
    avg_daily_use DOUBLE PRECISION NOT NULL DEFAULT 0,
    days_left     DOUBLE PRECISION,
    suggested_qty DOUBLE PRECISION NOT NULL DEFAULT 0,
    unit          VARCHAR(50),
    period        VARCHAR(20) NOT NULL DEFAULT 'WEEKLY',
    status        VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_ai_replenish_tenant ON ai_replenishment_hints(tenant_id, status);

-- -----------------------------------------------------------
-- 2. ai_demand_forecasts — per-product demand forecast (FR-12.7)
-- -----------------------------------------------------------
CREATE TABLE ai_demand_forecasts (
    forecast_id   VARCHAR(255) PRIMARY KEY,
    tenant_id     VARCHAR(255) NOT NULL,
    product_id    VARCHAR(255) NOT NULL,
    product_name  VARCHAR(255),
    period_start  DATE NOT NULL,
    period_end    DATE NOT NULL,
    forecast_qty  DOUBLE PRECISION NOT NULL,
    confidence    DOUBLE PRECISION,
    based_on_days INT NOT NULL DEFAULT 30,
    created_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_ai_forecast_tenant ON ai_demand_forecasts(tenant_id, product_id, period_start);

-- -----------------------------------------------------------
-- 3. ai_production_suggestions — batch/sequencing hints (FR-12.4)
-- -----------------------------------------------------------
CREATE TABLE ai_production_suggestions (
    suggestion_id    VARCHAR(255) PRIMARY KEY,
    tenant_id        VARCHAR(255) NOT NULL,
    product_id       VARCHAR(255) NOT NULL,
    product_name     VARCHAR(255),
    suggested_qty    DOUBLE PRECISION NOT NULL,
    suggested_batches INT NOT NULL DEFAULT 1,
    reason           VARCHAR(500),
    plan_date        DATE,
    status           VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_ai_production_tenant ON ai_production_suggestions(tenant_id, plan_date);

-- -----------------------------------------------------------
-- 4. driver_sessions — driver mobile sessions (FR-7.7)
-- -----------------------------------------------------------
CREATE TABLE driver_sessions (
    session_id   VARCHAR(255) PRIMARY KEY,
    tenant_id    VARCHAR(255) NOT NULL,
    driver_id    VARCHAR(255) NOT NULL,
    driver_name  VARCHAR(255),
    run_id       VARCHAR(255) REFERENCES delivery_runs(run_id),
    status       VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    started_at   TIMESTAMP WITH TIME ZONE,
    ended_at     TIMESTAMP WITH TIME ZONE,
    lat          DOUBLE PRECISION,
    lng          DOUBLE PRECISION,
    updated_at   TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_driver_session_tenant ON driver_sessions(tenant_id, status);

-- -----------------------------------------------------------
-- 5. driver_stop_updates — per-stop delivery updates (FR-7.7)
-- -----------------------------------------------------------
CREATE TABLE driver_stop_updates (
    update_id       VARCHAR(255) PRIMARY KEY,
    session_id      VARCHAR(255) NOT NULL REFERENCES driver_sessions(session_id),
    tenant_id       VARCHAR(255) NOT NULL,
    run_order_id    VARCHAR(255) NOT NULL,
    action          VARCHAR(50) NOT NULL,
    notes           VARCHAR(500),
    lat             DOUBLE PRECISION,
    lng             DOUBLE PRECISION,
    created_at      TIMESTAMP WITH TIME ZONE
);

-- -----------------------------------------------------------
-- 6. packaging_confirmations — pre-departure check (FR-8.7)
-- -----------------------------------------------------------
CREATE TABLE packaging_confirmations (
    confirmation_id VARCHAR(255) PRIMARY KEY,
    tenant_id       VARCHAR(255) NOT NULL,
    run_id          VARCHAR(255) NOT NULL REFERENCES delivery_runs(run_id),
    driver_id       VARCHAR(255) NOT NULL,
    all_confirmed   BOOLEAN NOT NULL DEFAULT FALSE,
    discrepancies   TEXT,
    confirmed_at    TIMESTAMP WITH TIME ZONE
);

-- -----------------------------------------------------------
-- 7. driver_payments — on-spot payment collection (FR-8.8)
-- -----------------------------------------------------------
CREATE TABLE driver_payments (
    payment_id     VARCHAR(255) PRIMARY KEY,
    tenant_id      VARCHAR(255) NOT NULL,
    session_id     VARCHAR(255) REFERENCES driver_sessions(session_id),
    order_id       VARCHAR(255) NOT NULL,
    invoice_id     VARCHAR(255),
    amount         DECIMAL(18,4) NOT NULL,
    payment_method VARCHAR(50) NOT NULL DEFAULT 'CASH',
    reference      VARCHAR(255),
    collected_at   TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_driver_payment_tenant ON driver_payments(tenant_id, order_id);
