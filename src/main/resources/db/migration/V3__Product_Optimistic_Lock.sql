-- Optimistic locking column for Product to prevent concurrent stock oversell.
ALTER TABLE product
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
