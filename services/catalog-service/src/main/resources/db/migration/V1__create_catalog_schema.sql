-- =============================================================================
-- V1__create_catalog_schema.sql
-- Isolated Schema for catalog-service (categories & products)
-- Uses UUIDv7 Primary Keys for sequential B+Tree index locality at 5M+ scale
-- =============================================================================

CREATE TABLE categories (
    id          UUID                        NOT NULL,
    name        VARCHAR(120)                NOT NULL,
    slug        VARCHAR(140)                NOT NULL,
    description VARCHAR(500),
    active      BOOLEAN                     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE    NOT NULL,
    version     BIGINT                      NOT NULL DEFAULT 0,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uq_categories_slug UNIQUE (slug)
);

CREATE TABLE products (
    id              UUID                        NOT NULL,
    sku             VARCHAR(64)                 NOT NULL,
    name            VARCHAR(200)                NOT NULL,
    description     VARCHAR(2000),
    price           NUMERIC(12, 2)              NOT NULL,
    currency        VARCHAR(3)                  NOT NULL DEFAULT 'USD',
    category_id     UUID                        NOT NULL,
    brand           VARCHAR(100),
    image_url       VARCHAR(500),
    popularity_score BIGINT                     NOT NULL DEFAULT 0,
    active          BOOLEAN                     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE    NOT NULL,
    version         BIGINT                      NOT NULL DEFAULT 0,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT chk_products_price_positive CHECK (price >= 0)
);

-- B+Tree Composite Index for Keyset/Cursor Pagination (avoids slow OFFSET scans on millions of products)
CREATE INDEX idx_products_active_id_desc ON products (active, id DESC);

-- B+Tree Composite Index for Category Filtering + Keyset Pagination
CREATE INDEX idx_products_category_active_id ON products (category_id, active, id DESC);
