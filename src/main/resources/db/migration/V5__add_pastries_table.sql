-- V5__add_pastries_table.sql
-- Creates the pastries table.
-- NOTE: orderType is NOT stored in daily_balances because a daily balance
-- aggregates many orders of both types; orderType lives in the DTO layer only.

CREATE TABLE IF NOT EXISTS pastries (
    id          BIGSERIAL       NOT NULL,
    name        VARCHAR(255)    NOT NULL,
    description VARCHAR(500),
    price       NUMERIC(10, 2)  NOT NULL,
    available   BOOLEAN         NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_pastries PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_pastries_name      ON pastries (name);
CREATE INDEX IF NOT EXISTS idx_pastries_available ON pastries (available);
