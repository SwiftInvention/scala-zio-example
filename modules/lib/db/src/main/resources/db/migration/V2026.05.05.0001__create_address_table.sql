CREATE TABLE address (
    id          VARCHAR(64)  NOT NULL,
    customer_id VARCHAR(64)  NOT NULL,
    line        VARCHAR(255) NOT NULL,
    city        VARCHAR(120) NOT NULL,
    postal_code VARCHAR(40)  NOT NULL,
    PRIMARY KEY (id),
    KEY ix_address_customer (customer_id),
    CONSTRAINT fk_address_customer
        FOREIGN KEY (customer_id) REFERENCES customer (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
