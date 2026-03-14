-- V9: Tenant onboarding requests and branding tables (D4.1, D4.3)

CREATE TABLE IF NOT EXISTS tenant_onboarding_requests (
    request_id        VARCHAR(255) NOT NULL PRIMARY KEY,
    tenant_slug       VARCHAR(255) NOT NULL UNIQUE,
    business_name     VARCHAR(255) NOT NULL,
    owner_email       VARCHAR(255) NOT NULL,
    owner_name        VARCHAR(255),
    owner_phone       VARCHAR(255),
    country           VARCHAR(255),
    currency          VARCHAR(255),
    requested_tier    VARCHAR(255) DEFAULT 'BASIC',
    status            VARCHAR(255) DEFAULT 'PENDING',
    rejection_reason  VARCHAR(255),
    provisioned_tenant_id VARCHAR(255),
    created_at        TIMESTAMP WITH TIME ZONE,
    approved_at       TIMESTAMP WITH TIME ZONE,
    provisioned_at    TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS tenant_branding (
    tenant_id              VARCHAR(255) NOT NULL PRIMARY KEY,
    logo_url               VARCHAR(1024),
    primary_color          VARCHAR(255) DEFAULT '#2563eb',
    secondary_color        VARCHAR(255) DEFAULT '#1e40af',
    accent_color           VARCHAR(255) DEFAULT '#f59e0b',
    receipt_business_name  VARCHAR(255),
    receipt_footer         TEXT,
    receipt_header         VARCHAR(255),
    locale                 VARCHAR(50)  DEFAULT 'en',
    timezone               VARCHAR(100) DEFAULT 'UTC',
    updated_at             TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_onboarding_status ON tenant_onboarding_requests(status);
CREATE INDEX IF NOT EXISTS idx_onboarding_email ON tenant_onboarding_requests(owner_email);

-- D4.4: Tenant suspension support
ALTER TABLE tenant_config ADD COLUMN IF NOT EXISTS suspended BOOLEAN DEFAULT FALSE;
ALTER TABLE tenant_config ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMP WITH TIME ZONE;
