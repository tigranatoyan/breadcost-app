-- Recipe template system for reusable recipe + technology step blueprints
-- Supports multi-industry flexibility (bread, pastry, fast food, etc.)

CREATE TABLE recipe_templates (
    template_id     VARCHAR(36)     PRIMARY KEY,
    tenant_id       VARCHAR(36)     NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    description     VARCHAR(2000),
    category        VARCHAR(100)    NOT NULL DEFAULT 'General',
    batch_size      NUMERIC(19,4)   NOT NULL,
    batch_size_uom  VARCHAR(20)     NOT NULL,
    expected_yield  NUMERIC(19,4)   NOT NULL,
    yield_uom       VARCHAR(20)     NOT NULL,
    production_notes VARCHAR(2000),
    lead_time_hours INTEGER,
    created_by      VARCHAR(200),
    created_at_utc  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at_utc  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tpl_tenant ON recipe_templates(tenant_id);
CREATE INDEX idx_tpl_category ON recipe_templates(tenant_id, category);

CREATE TABLE recipe_template_ingredients (
    ingredient_line_id  VARCHAR(36)     PRIMARY KEY,
    template_id         VARCHAR(36)     NOT NULL REFERENCES recipe_templates(template_id) ON DELETE CASCADE,
    tenant_id           VARCHAR(36),
    item_id             VARCHAR(36)     NOT NULL,
    item_name           VARCHAR(200),
    unit_mode           VARCHAR(10)     NOT NULL,
    recipe_qty          NUMERIC(19,4),
    recipe_uom          VARCHAR(20),
    piece_qty           INTEGER,
    weight_per_piece    NUMERIC(19,4),
    piece_weight_uom    VARCHAR(20),
    purchasing_unit_size NUMERIC(19,4),
    purchasing_uom      VARCHAR(20),
    waste_factor        NUMERIC(7,4)
);

CREATE INDEX idx_tpli_template ON recipe_template_ingredients(template_id);

CREATE TABLE recipe_template_steps (
    step_id             VARCHAR(36)     PRIMARY KEY,
    template_id         VARCHAR(36)     NOT NULL REFERENCES recipe_templates(template_id) ON DELETE CASCADE,
    tenant_id           VARCHAR(36),
    step_number         INTEGER         NOT NULL,
    name                VARCHAR(200)    NOT NULL,
    activities          VARCHAR(2000),
    instruments         VARCHAR(500),
    duration_minutes    INTEGER,
    temperature_celsius INTEGER
);

CREATE INDEX idx_tpls_template ON recipe_template_steps(template_id);
