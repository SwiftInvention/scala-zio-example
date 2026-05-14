CREATE TABLE notification (
    id           VARCHAR(64)  NOT NULL,
    recipient_id VARCHAR(64)  NOT NULL,
    channel      VARCHAR(16)  NOT NULL,
    message      VARCHAR(2000) NOT NULL,
    created_at   DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY ix_notification_recipient (recipient_id),
    CONSTRAINT fk_notification_recipient
        FOREIGN KEY (recipient_id) REFERENCES customer (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
