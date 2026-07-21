CREATE TABLE orders (
    id               VARCHAR(36)   NOT NULL,
    user_id          VARCHAR(36)   NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    total_amount     DECIMAL(12,2) NOT NULL,
    shipping_address VARCHAR(500)  NOT NULL,
    created_at       TIMESTAMP     NOT NULL,
    updated_at       TIMESTAMP     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_orders_user_id ON orders (user_id);

CREATE TABLE order_items (
    id                VARCHAR(36)   NOT NULL,
    order_id          VARCHAR(36)   NOT NULL,
    product_id        VARCHAR(36)   NOT NULL,
    product_name      VARCHAR(255)  NOT NULL,
    quantity          INT           NOT NULL,
    price_at_purchase DECIMAL(12,2) NOT NULL,
    created_at        TIMESTAMP     NOT NULL,
    updated_at        TIMESTAMP     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
