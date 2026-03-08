-- ============================================================
-- Flyway V1 — Initial Schema Migration for BreadCost
-- ============================================================
-- Generated from 36 JPA @Entity classes + 1 @ElementCollection.
-- Source: Hibernate ddl-auto=update schema on H2, ported to PostgreSQL.
-- Spring Boot naming strategy: CamelCaseToUnderscoresNamingStrategy
--   (all camelCase field/column names → snake_case)
-- ============================================================

-- -----------------------------------------------------------
-- 1. tenant_config  (TenantConfigEntity)
-- -----------------------------------------------------------
CREATE TABLE tenant_config (
    tenant_id              VARCHAR(255) PRIMARY KEY,
    display_name           VARCHAR(255),
    order_cutoff_time      VARCHAR(255) DEFAULT '22:00',
    rush_order_premium_pct DOUBLE PRECISION NOT NULL DEFAULT 15.0,
    main_currency          VARCHAR(255) DEFAULT 'UZS',
    updated_at             TIMESTAMP WITH TIME ZONE
);

-- -----------------------------------------------------------
-- 2. app_users  (UserEntity)
-- -----------------------------------------------------------
CREATE TABLE app_users (
    user_id       VARCHAR(255) PRIMARY KEY,
    tenant_id     VARCHAR(255) NOT NULL,
    username      VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(255),
    roles         VARCHAR(500),
    department_id VARCHAR(255),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE,
    updated_at    TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_app_users_tenant_username UNIQUE (tenant_id, username)
);

CREATE INDEX idx_app_users_tenant_id ON app_users (tenant_id);

-- -----------------------------------------------------------
-- 3. departments  (DepartmentEntity)
-- -----------------------------------------------------------
CREATE TABLE departments (
    department_id  VARCHAR(255) PRIMARY KEY,
    tenant_id      VARCHAR(255) NOT NULL,
    name           VARCHAR(255) NOT NULL,
    lead_time_hours INTEGER,
    warehouse_mode VARCHAR(50) NOT NULL,
    status         VARCHAR(50) NOT NULL,
    created_at_utc TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at_utc TIMESTAMP WITH TIME ZONE,
    created_by     VARCHAR(255),
    CONSTRAINT uq_departments_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_departments_tenant_id ON departments (tenant_id);

-- -----------------------------------------------------------
-- 4. items  (ItemEntity)
-- -----------------------------------------------------------
CREATE TABLE items (
    item_id             VARCHAR(255) PRIMARY KEY,
    tenant_id           VARCHAR(255) NOT NULL,
    name                VARCHAR(200) NOT NULL,
    type                VARCHAR(50) NOT NULL,
    base_uom            VARCHAR(20) NOT NULL,
    description         TEXT,
    min_stock_threshold DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE,
    updated_at          TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_items_tenant_id ON items (tenant_id);

-- -----------------------------------------------------------
-- 5. products  (ProductEntity)
-- -----------------------------------------------------------
CREATE TABLE products (
    product_id       VARCHAR(255) PRIMARY KEY,
    tenant_id        VARCHAR(255) NOT NULL,
    department_id    VARCHAR(255) NOT NULL,
    name             VARCHAR(255) NOT NULL,
    description      VARCHAR(255),
    sale_unit        VARCHAR(50) NOT NULL,
    base_uom         VARCHAR(255) NOT NULL,
    price            NUMERIC(18, 4),
    vat_rate_pct     DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    active_recipe_id VARCHAR(255),
    status           VARCHAR(50) NOT NULL,
    created_at_utc   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at_utc   TIMESTAMP WITH TIME ZONE,
    created_by       VARCHAR(255),
    CONSTRAINT uq_products_tenant_name_dept UNIQUE (tenant_id, name, department_id)
);

CREATE INDEX idx_products_tenant_id ON products (tenant_id);

-- -----------------------------------------------------------
-- 6. recipes  (RecipeEntity)
-- -----------------------------------------------------------
CREATE TABLE recipes (
    recipe_id        VARCHAR(255) PRIMARY KEY,
    tenant_id        VARCHAR(255) NOT NULL,
    product_id       VARCHAR(255) NOT NULL,
    version_number   INTEGER NOT NULL,
    status           VARCHAR(50) NOT NULL,
    batch_size       NUMERIC(19, 4) NOT NULL,
    batch_size_uom   VARCHAR(255) NOT NULL,
    expected_yield   NUMERIC(19, 4) NOT NULL,
    yield_uom        VARCHAR(255) NOT NULL,
    production_notes VARCHAR(2000),
    lead_time_hours  INTEGER,
    created_at_utc   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at_utc   TIMESTAMP WITH TIME ZONE,
    created_by       VARCHAR(255),
    CONSTRAINT uq_recipes_tenant_product_version UNIQUE (tenant_id, product_id, version_number)
);

CREATE INDEX idx_recipes_tenant_id ON recipes (tenant_id);

-- -----------------------------------------------------------
-- 7. recipe_ingredients  (RecipeIngredientEntity)
-- -----------------------------------------------------------
CREATE TABLE recipe_ingredients (
    ingredient_line_id   VARCHAR(255) PRIMARY KEY,
    tenant_id            VARCHAR(255),
    recipe_id            VARCHAR(255) NOT NULL,
    item_id              VARCHAR(255) NOT NULL,
    item_name            VARCHAR(255),
    unit_mode            VARCHAR(50) NOT NULL,
    recipe_qty           NUMERIC(19, 4),
    recipe_uom           VARCHAR(255),
    piece_qty            INTEGER,
    weight_per_piece     NUMERIC(19, 4),
    piece_weight_uom     VARCHAR(255),
    purchasing_unit_size NUMERIC(19, 4),
    purchasing_uom       VARCHAR(255),
    waste_factor         NUMERIC(7, 4)
);

CREATE INDEX idx_recipe_ingredients_tenant_id ON recipe_ingredients (tenant_id);

-- -----------------------------------------------------------
-- 8. technology_steps  (TechnologyStepEntity)
-- -----------------------------------------------------------
CREATE TABLE technology_steps (
    step_id             VARCHAR(255) PRIMARY KEY,
    tenant_id           VARCHAR(255) NOT NULL,
    recipe_id           VARCHAR(255) NOT NULL,
    step_number         INTEGER NOT NULL,
    name                VARCHAR(200) NOT NULL,
    activities          VARCHAR(2000),
    instruments         VARCHAR(500),
    duration_minutes    INTEGER,
    temperature_celsius INTEGER,
    created_at_utc      TIMESTAMP WITH TIME ZONE,
    updated_at_utc      TIMESTAMP WITH TIME ZONE
);

-- Entity-declared index: @Index(name = "idx_ts_tenant_recipe")
CREATE INDEX idx_ts_tenant_recipe ON technology_steps (tenant_id, recipe_id);

-- -----------------------------------------------------------
-- 9. orders  (OrderEntity)
-- -----------------------------------------------------------
CREATE TABLE orders (
    order_id                VARCHAR(255) PRIMARY KEY,
    tenant_id               VARCHAR(255) NOT NULL,
    site_id                 VARCHAR(255),
    customer_id             VARCHAR(255),
    customer_name           VARCHAR(255),
    created_by_user_id      VARCHAR(255),
    status                  VARCHAR(255) NOT NULL,
    requested_delivery_time TIMESTAMP WITH TIME ZONE,
    order_placed_at         TIMESTAMP WITH TIME ZONE,
    confirmed_at            TIMESTAMP WITH TIME ZONE,
    rush_order              BOOLEAN NOT NULL DEFAULT FALSE,
    rush_premium_pct        NUMERIC(10, 4),
    notes                   TEXT,
    total_amount            NUMERIC(18, 4),
    created_at              TIMESTAMP WITH TIME ZONE,
    updated_at              TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_orders_tenant_id ON orders (tenant_id);

-- -----------------------------------------------------------
-- 10. order_lines  (OrderLineEntity)
-- -----------------------------------------------------------
CREATE TABLE order_lines (
    order_line_id      VARCHAR(255) PRIMARY KEY,
    tenant_id          VARCHAR(255),
    order_id           VARCHAR(255) NOT NULL,
    product_id         VARCHAR(255),
    product_name       VARCHAR(255),
    department_id      VARCHAR(255),
    department_name    VARCHAR(255),
    qty                DOUBLE PRECISION NOT NULL,
    uom                VARCHAR(255),
    unit_price         NUMERIC(18, 4),
    lead_time_conflict BOOLEAN NOT NULL DEFAULT FALSE,
    earliest_ready_at  TIMESTAMP WITH TIME ZONE,
    notes              TEXT,
    CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id) REFERENCES orders (order_id)
);

CREATE INDEX idx_order_lines_tenant_id ON order_lines (tenant_id);

-- -----------------------------------------------------------
-- 11. production_plans  (ProductionPlanEntity)
-- -----------------------------------------------------------
CREATE TABLE production_plans (
    plan_id            VARCHAR(255) PRIMARY KEY,
    tenant_id          VARCHAR(255) NOT NULL,
    site_id            VARCHAR(255),
    plan_date          DATE NOT NULL,
    shift              VARCHAR(50),
    status             VARCHAR(50) NOT NULL,
    created_by_user_id VARCHAR(255),
    notes              TEXT,
    created_at         TIMESTAMP WITH TIME ZONE,
    updated_at         TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_production_plans_tenant_id ON production_plans (tenant_id);

-- -----------------------------------------------------------
-- 12. work_orders  (WorkOrderEntity)
-- -----------------------------------------------------------
CREATE TABLE work_orders (
    work_order_id         VARCHAR(255) PRIMARY KEY,
    plan_id               VARCHAR(255) NOT NULL,
    tenant_id             VARCHAR(255) NOT NULL,
    department_id         VARCHAR(255),
    department_name       VARCHAR(255),
    product_id            VARCHAR(255),
    product_name          VARCHAR(255),
    recipe_id             VARCHAR(255),
    target_qty            DOUBLE PRECISION NOT NULL,
    uom                   VARCHAR(255),
    batch_count           INTEGER NOT NULL,
    status                VARCHAR(50) NOT NULL,
    assigned_to_user_id   VARCHAR(255),
    started_at            TIMESTAMP WITH TIME ZONE,
    completed_at          TIMESTAMP WITH TIME ZONE,
    source_order_line_ids TEXT,
    notes                 TEXT,
    start_offset_hours    INTEGER DEFAULT 0,
    duration_hours        INTEGER,
    CONSTRAINT fk_work_orders_plan FOREIGN KEY (plan_id) REFERENCES production_plans (plan_id)
);

CREATE INDEX idx_work_orders_tenant_id ON work_orders (tenant_id);

-- -----------------------------------------------------------
-- 13. pos_sales  (SaleEntity)
-- -----------------------------------------------------------
CREATE TABLE pos_sales (
    sale_id        VARCHAR(255) PRIMARY KEY,
    tenant_id      VARCHAR(255) NOT NULL,
    site_id        VARCHAR(255),
    cashier_id     VARCHAR(255),
    cashier_name   VARCHAR(255),
    status         VARCHAR(50) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    subtotal       NUMERIC(18, 4),
    total_amount   NUMERIC(18, 4),
    cash_received  NUMERIC(18, 4),
    change_given   NUMERIC(18, 4),
    card_reference VARCHAR(255),
    completed_at   TIMESTAMP WITH TIME ZONE,
    created_at     TIMESTAMP WITH TIME ZONE,
    updated_at     TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_pos_sales_tenant_id ON pos_sales (tenant_id);

-- -----------------------------------------------------------
-- 14. pos_sale_lines  (SaleLineEntity)
-- -----------------------------------------------------------
CREATE TABLE pos_sale_lines (
    line_id      VARCHAR(255) PRIMARY KEY,
    tenant_id    VARCHAR(255),
    sale_id      VARCHAR(255),
    product_id   VARCHAR(255),
    product_name VARCHAR(255),
    quantity     NUMERIC(14, 4),
    unit         VARCHAR(255),
    unit_price   NUMERIC(14, 4),
    line_total   NUMERIC(14, 4),
    CONSTRAINT fk_pos_sale_lines_sale FOREIGN KEY (sale_id) REFERENCES pos_sales (sale_id)
);

CREATE INDEX idx_pos_sale_lines_tenant_id ON pos_sale_lines (tenant_id);

-- -----------------------------------------------------------
-- 15. customers  (CustomerEntity)
-- -----------------------------------------------------------
CREATE TABLE customers (
    customer_id         VARCHAR(255) PRIMARY KEY,
    tenant_id           VARCHAR(255) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    password_hash       VARCHAR(255),
    phone               VARCHAR(255),
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    payment_terms_days  INTEGER NOT NULL DEFAULT 30,
    credit_limit        NUMERIC(19, 2) DEFAULT 0,
    outstanding_balance NUMERIC(19, 2) DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE,
    updated_at          TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_customer_tenant_email UNIQUE (tenant_id, email)
);

CREATE INDEX idx_customers_tenant_id ON customers (tenant_id);

-- -----------------------------------------------------------
-- 16. customer_addresses  (@ElementCollection from CustomerEntity)
-- -----------------------------------------------------------
CREATE TABLE customer_addresses (
    customer_id  VARCHAR(255) NOT NULL,
    label        VARCHAR(255),
    line1        VARCHAR(255),
    line2        VARCHAR(255),
    city         VARCHAR(255),
    postal_code  VARCHAR(255),
    country_code VARCHAR(255),
    CONSTRAINT fk_customer_addresses_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id)
);

-- -----------------------------------------------------------
-- 17. customer_discount_rules  (CustomerDiscountRuleEntity)
-- -----------------------------------------------------------
CREATE TABLE customer_discount_rules (
    rule_id      VARCHAR(255) PRIMARY KEY,
    tenant_id    VARCHAR(255) NOT NULL,
    customer_id  VARCHAR(255) NOT NULL,
    item_type    VARCHAR(50) DEFAULT 'PRODUCT',
    item_id      VARCHAR(255),
    discount_pct NUMERIC(19, 2) DEFAULT 0,
    fixed_price  NUMERIC(19, 2),
    min_qty      NUMERIC(19, 2) DEFAULT 1,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    notes        VARCHAR(255),
    created_at   TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_customer_discount_rules_tenant_id ON customer_discount_rules (tenant_id);

-- -----------------------------------------------------------
-- 18. invoices  (InvoiceEntity)
-- -----------------------------------------------------------
CREATE TABLE invoices (
    invoice_id         VARCHAR(255) PRIMARY KEY,
    tenant_id          VARCHAR(255) NOT NULL,
    customer_id        VARCHAR(255) NOT NULL,
    order_id           VARCHAR(255) NOT NULL,
    status             VARCHAR(50) DEFAULT 'DRAFT',
    invoice_number     VARCHAR(255),
    subtotal           NUMERIC(19, 2) DEFAULT 0,
    tax_amount         NUMERIC(19, 2) DEFAULT 0,
    total_amount       NUMERIC(19, 2) DEFAULT 0,
    currency_code      VARCHAR(255),
    payment_terms_days INTEGER NOT NULL DEFAULT 30,
    issued_date        DATE,
    due_date           DATE,
    paid_at            TIMESTAMP WITH TIME ZONE,
    paid_by            VARCHAR(255),
    paid_amount        NUMERIC(19, 2),
    notes              VARCHAR(255),
    created_at         TIMESTAMP WITH TIME ZONE,
    updated_at         TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_invoices_tenant_id ON invoices (tenant_id);

-- -----------------------------------------------------------
-- 19. invoice_lines  (InvoiceLineEntity)
-- -----------------------------------------------------------
CREATE TABLE invoice_lines (
    line_id      VARCHAR(255) PRIMARY KEY,
    invoice_id   VARCHAR(255) NOT NULL,
    tenant_id    VARCHAR(255) NOT NULL,
    product_id   VARCHAR(255),
    product_name VARCHAR(255),
    qty          NUMERIC(19, 2) DEFAULT 1,
    unit         VARCHAR(255),
    unit_price   NUMERIC(19, 2) DEFAULT 0,
    discount_pct NUMERIC(19, 2) DEFAULT 0,
    line_total   NUMERIC(19, 2) DEFAULT 0
);

CREATE INDEX idx_invoice_lines_tenant_id ON invoice_lines (tenant_id);

-- -----------------------------------------------------------
-- 20. loyalty_accounts  (LoyaltyAccountEntity)
-- -----------------------------------------------------------
CREATE TABLE loyalty_accounts (
    account_id      VARCHAR(255) PRIMARY KEY,
    tenant_id       VARCHAR(255) NOT NULL,
    customer_id     VARCHAR(255) NOT NULL,
    points_balance  BIGINT NOT NULL DEFAULT 0,
    points_earned   BIGINT NOT NULL DEFAULT 0,
    points_redeemed BIGINT NOT NULL DEFAULT 0,
    tier_name       VARCHAR(255) DEFAULT 'Bronze',
    created_at      TIMESTAMP WITH TIME ZONE,
    updated_at      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_loyalty_tenant_customer UNIQUE (tenant_id, customer_id)
);

CREATE INDEX idx_loyalty_accounts_tenant_id ON loyalty_accounts (tenant_id);

-- -----------------------------------------------------------
-- 21. loyalty_tiers  (LoyaltyTierEntity)
-- -----------------------------------------------------------
CREATE TABLE loyalty_tiers (
    tier_id              VARCHAR(255) PRIMARY KEY,
    tenant_id            VARCHAR(255) NOT NULL,
    name                 VARCHAR(255) NOT NULL,
    min_points           BIGINT NOT NULL DEFAULT 0,
    discount_pct         DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    points_per_dollar    DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    benefits_description VARCHAR(255),
    created_at           TIMESTAMP WITH TIME ZONE,
    updated_at           TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_loyalty_tier_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_loyalty_tiers_tenant_id ON loyalty_tiers (tenant_id);

-- -----------------------------------------------------------
-- 22. loyalty_transactions  (LoyaltyTransactionEntity)
-- -----------------------------------------------------------
CREATE TABLE loyalty_transactions (
    tx_id       VARCHAR(255) PRIMARY KEY,
    account_id  VARCHAR(255) NOT NULL,
    tenant_id   VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    type        VARCHAR(255) NOT NULL,
    points      BIGINT NOT NULL,
    order_id    VARCHAR(255),
    description VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_loyalty_transactions_tenant_id ON loyalty_transactions (tenant_id);

-- -----------------------------------------------------------
-- 23. suppliers  (SupplierEntity)
-- -----------------------------------------------------------
CREATE TABLE suppliers (
    supplier_id   VARCHAR(255) PRIMARY KEY,
    tenant_id     VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(255),
    notes         VARCHAR(2000),
    created_at    TIMESTAMP WITH TIME ZONE,
    updated_at    TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_supplier_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_suppliers_tenant_id ON suppliers (tenant_id);

-- -----------------------------------------------------------
-- 24. supplier_catalog_items  (SupplierCatalogItemEntity)
-- -----------------------------------------------------------
CREATE TABLE supplier_catalog_items (
    item_id         VARCHAR(255) PRIMARY KEY,
    supplier_id     VARCHAR(255) NOT NULL,
    tenant_id       VARCHAR(255) NOT NULL,
    ingredient_id   VARCHAR(255) NOT NULL,
    ingredient_name VARCHAR(255),
    unit_price      NUMERIC(19, 2) DEFAULT 0,
    currency        VARCHAR(255) DEFAULT 'USD',
    lead_time_days  INTEGER NOT NULL DEFAULT 1,
    moq             DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    unit            VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE,
    updated_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_supplier_catalog_items_tenant_id ON supplier_catalog_items (tenant_id);

-- -----------------------------------------------------------
-- 25. purchase_orders  (PurchaseOrderEntity)
-- -----------------------------------------------------------
CREATE TABLE purchase_orders (
    po_id            VARCHAR(255) PRIMARY KEY,
    tenant_id        VARCHAR(255) NOT NULL,
    supplier_id      VARCHAR(255) NOT NULL,
    supplier_name    VARCHAR(255),
    status           VARCHAR(50) DEFAULT 'DRAFT',
    notes            VARCHAR(2000),
    fx_rate          DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    fx_currency_code VARCHAR(255) DEFAULT 'USD',
    total_amount     NUMERIC(19, 2) DEFAULT 0,
    approved_by      VARCHAR(255),
    approved_at      TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE,
    updated_at       TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_purchase_orders_tenant_id ON purchase_orders (tenant_id);

-- -----------------------------------------------------------
-- 26. purchase_order_lines  (PurchaseOrderLineEntity)
-- -----------------------------------------------------------
CREATE TABLE purchase_order_lines (
    line_id         VARCHAR(255) PRIMARY KEY,
    po_id           VARCHAR(255) NOT NULL,
    tenant_id       VARCHAR(255) NOT NULL,
    ingredient_id   VARCHAR(255),
    ingredient_name VARCHAR(255),
    qty             DOUBLE PRECISION NOT NULL DEFAULT 0,
    unit            VARCHAR(255),
    unit_price      NUMERIC(19, 2) DEFAULT 0,
    currency        VARCHAR(255) DEFAULT 'USD',
    line_total      NUMERIC(19, 2) DEFAULT 0
);

CREATE INDEX idx_purchase_order_lines_tenant_id ON purchase_order_lines (tenant_id);

-- -----------------------------------------------------------
-- 27. supplier_deliveries  (SupplierDeliveryEntity)
-- -----------------------------------------------------------
CREATE TABLE supplier_deliveries (
    delivery_id     VARCHAR(255) PRIMARY KEY,
    tenant_id       VARCHAR(255) NOT NULL,
    po_id           VARCHAR(255),
    supplier_id     VARCHAR(255),
    status          VARCHAR(50) DEFAULT 'RECEIVED',
    has_discrepancy BOOLEAN NOT NULL DEFAULT FALSE,
    notes           VARCHAR(2000),
    received_at     TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_supplier_deliveries_tenant_id ON supplier_deliveries (tenant_id);

-- -----------------------------------------------------------
-- 28. supplier_delivery_lines  (SupplierDeliveryLineEntity)
-- -----------------------------------------------------------
CREATE TABLE supplier_delivery_lines (
    line_id          VARCHAR(255) PRIMARY KEY,
    delivery_id      VARCHAR(255) NOT NULL,
    tenant_id        VARCHAR(255) NOT NULL,
    ingredient_id    VARCHAR(255),
    ingredient_name  VARCHAR(255),
    qty_ordered      DOUBLE PRECISION NOT NULL DEFAULT 0,
    qty_received     DOUBLE PRECISION NOT NULL DEFAULT 0,
    unit             VARCHAR(255),
    unit_price       NUMERIC(19, 2) DEFAULT 0,
    discrepancy      BOOLEAN NOT NULL DEFAULT FALSE,
    discrepancy_note VARCHAR(255)
);

CREATE INDEX idx_supplier_delivery_lines_tenant_id ON supplier_delivery_lines (tenant_id);

-- -----------------------------------------------------------
-- 29. delivery_runs  (DeliveryRunEntity)
-- -----------------------------------------------------------
CREATE TABLE delivery_runs (
    run_id         VARCHAR(255) PRIMARY KEY,
    tenant_id      VARCHAR(255) NOT NULL,
    driver_id      VARCHAR(255),
    driver_name    VARCHAR(255),
    scheduled_date DATE,
    status         VARCHAR(50) DEFAULT 'PENDING',
    courier_charge NUMERIC(19, 2) DEFAULT 0,
    notes          VARCHAR(2000),
    created_at     TIMESTAMP WITH TIME ZONE,
    updated_at     TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_delivery_runs_tenant_id ON delivery_runs (tenant_id);

-- -----------------------------------------------------------
-- 30. delivery_run_orders  (DeliveryRunOrderEntity)
-- -----------------------------------------------------------
CREATE TABLE delivery_run_orders (
    id                    VARCHAR(255) PRIMARY KEY,
    run_id                VARCHAR(255) NOT NULL,
    tenant_id             VARCHAR(255) NOT NULL,
    order_id              VARCHAR(255) NOT NULL,
    status                VARCHAR(50) DEFAULT 'PENDING',
    failure_reason        VARCHAR(255),
    re_delivery_run_id    VARCHAR(255),
    courier_charge        NUMERIC(19, 2) DEFAULT 0,
    courier_charge_waived BOOLEAN NOT NULL DEFAULT FALSE,
    waived_by             VARCHAR(255),
    completed_at          TIMESTAMP WITH TIME ZONE,
    created_at            TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_delivery_run_order UNIQUE (run_id, order_id)
);

CREATE INDEX idx_delivery_run_orders_tenant_id ON delivery_run_orders (tenant_id);

-- -----------------------------------------------------------
-- 31. custom_reports  (CustomReportEntity)
-- -----------------------------------------------------------
CREATE TABLE custom_reports (
    report_id   VARCHAR(255) PRIMARY KEY,
    tenant_id   VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    created_by  VARCHAR(255),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE,
    updated_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_custom_reports_tenant_id ON custom_reports (tenant_id);

-- -----------------------------------------------------------
-- 32. custom_report_blocks  (CustomReportBlockEntity)
-- -----------------------------------------------------------
CREATE TABLE custom_report_blocks (
    id                VARCHAR(255) PRIMARY KEY,
    tenant_id         VARCHAR(255),
    block_key         VARCHAR(255) NOT NULL,
    display_order     INTEGER NOT NULL DEFAULT 0,
    date_range_preset VARCHAR(255),
    report_id         VARCHAR(255),
    CONSTRAINT fk_custom_report_blocks_report FOREIGN KEY (report_id) REFERENCES custom_reports (report_id)
);

CREATE INDEX idx_custom_report_blocks_tenant_id ON custom_report_blocks (tenant_id);

-- -----------------------------------------------------------
-- 33. report_kpi_blocks  (ReportKpiBlockEntity — not tenant-scoped)
-- -----------------------------------------------------------
CREATE TABLE report_kpi_blocks (
    block_id    VARCHAR(255) PRIMARY KEY,
    block_key   VARCHAR(255) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    category    VARCHAR(50) DEFAULT 'FINANCIAL',
    query_type  VARCHAR(50) DEFAULT 'AGGREGATE',
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    unit        VARCHAR(255)
);

-- -----------------------------------------------------------
-- 34. subscription_tiers  (SubscriptionTierEntity — not tenant-scoped)
-- -----------------------------------------------------------
CREATE TABLE subscription_tiers (
    tier_id          VARCHAR(255) PRIMARY KEY,
    level            VARCHAR(50) NOT NULL UNIQUE,
    name             VARCHAR(255),
    description      VARCHAR(255),
    enabled_features VARCHAR(2000),
    max_users        INTEGER NOT NULL DEFAULT 0,
    max_products     INTEGER NOT NULL DEFAULT 0,
    created_at       TIMESTAMP WITH TIME ZONE
);

-- -----------------------------------------------------------
-- 35. tenant_subscriptions  (TenantSubscriptionEntity)
-- -----------------------------------------------------------
CREATE TABLE tenant_subscriptions (
    subscription_id VARCHAR(255) PRIMARY KEY,
    tenant_id       VARCHAR(255) NOT NULL,
    tier_level      VARCHAR(50) NOT NULL,
    start_date      DATE,
    expiry_date     DATE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_by     VARCHAR(255),
    assigned_at     TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_tenant_subscriptions_tenant_id ON tenant_subscriptions (tenant_id);

-- -----------------------------------------------------------
-- 36. batch_cost_view  (BatchCostView — projection read-model)
-- -----------------------------------------------------------
CREATE TABLE batch_cost_view (
    batch_id                  VARCHAR(255) PRIMARY KEY,
    tenant_id                 VARCHAR(255),
    site_id                   VARCHAR(255),
    status                    VARCHAR(255),
    total_material_cost       NUMERIC(19, 4),
    total_labor_cost          NUMERIC(19, 4),
    total_overhead_cost       NUMERIC(19, 4),
    total_cost                NUMERIC(19, 4),
    last_processed_ledger_seq BIGINT
);

CREATE INDEX idx_batch_cost_view_tenant_id ON batch_cost_view (tenant_id);

-- -----------------------------------------------------------
-- 37. inventory_valuation_view  (InventoryValuationView — projection read-model)
-- -----------------------------------------------------------
CREATE TABLE inventory_valuation_view (
    id                        BIGSERIAL PRIMARY KEY,
    tenant_id                 VARCHAR(255),
    site_id                   VARCHAR(255),
    item_id                   VARCHAR(255),
    lot_id                    VARCHAR(255),
    location_id               VARCHAR(255),
    on_hand_qty               NUMERIC(19, 4),
    valuation_amount          NUMERIC(19, 4),
    avg_unit_cost             NUMERIC(19, 4),
    last_processed_ledger_seq BIGINT
);

CREATE INDEX idx_inventory_valuation_view_tenant_id ON inventory_valuation_view (tenant_id);
