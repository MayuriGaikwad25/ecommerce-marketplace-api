CREATE TABLE products (
    id             VARCHAR(36)    NOT NULL,
    name           VARCHAR(255)   NOT NULL,
    description    TEXT,
    price          DECIMAL(12,2)  NOT NULL,
    stock_quantity INT            NOT NULL,
    sku            VARCHAR(100)   NOT NULL,
    active         BOOLEAN        NOT NULL DEFAULT TRUE,
    category_id    VARCHAR(36)    NOT NULL,
    vendor_id      VARCHAR(36)    NOT NULL,
    version        BIGINT         NOT NULL DEFAULT 0,
    created_at     TIMESTAMP      NOT NULL,
    updated_at     TIMESTAMP      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_products_vendor FOREIGN KEY (vendor_id) REFERENCES users (id)
);

CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_vendor_id ON products (vendor_id);
