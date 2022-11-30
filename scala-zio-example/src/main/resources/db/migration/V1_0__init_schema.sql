CREATE TABLE IF NOT EXISTS person
(
    name       varchar(255),
    birth_date DATETIME NOT NULL,
    PRIMARY KEY (name)
);

INSERT IGNORE INTO person(name, birth_date)
VALUES ('Martin Odersky', '1958-09-05 00:42:01');
INSERT IGNORE INTO person(name, birth_date)
VALUES ('James Gosling', '1955-05-19 01:42:01');
INSERT IGNORE INTO person(name, birth_date)
VALUES ('Mister X', now());