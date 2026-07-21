CREATE TABLE categories (
    id         VARCHAR(36)  NOT NULL,
    name       VARCHAR(255) NOT NULL,
    parent_id  VARCHAR(36),
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id)
);
