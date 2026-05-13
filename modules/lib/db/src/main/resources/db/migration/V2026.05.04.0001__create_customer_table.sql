CREATE TABLE customer (
    id    VARCHAR(64)  NOT NULL,
    email VARCHAR(255) NOT NULL,
    name  VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_customer_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
