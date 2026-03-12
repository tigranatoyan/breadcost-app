-- V5: R4-S2 — Password reset tokens

CREATE TABLE password_reset_tokens (
    id               VARCHAR(64)  PRIMARY KEY,
    customer_id      VARCHAR(64)  NOT NULL,
    token            VARCHAR(64)  NOT NULL UNIQUE,
    expires_at_epoch_ms BIGINT    NOT NULL,
    used             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at_epoch_ms BIGINT    NOT NULL
);

CREATE INDEX idx_prt_token ON password_reset_tokens(token);
CREATE INDEX idx_prt_customer ON password_reset_tokens(customer_id);
