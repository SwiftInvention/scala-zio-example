CREATE TABLE IF NOT EXISTS person (
    name varchar(255) NOT NULL,
    birth_date timestamp NOT NULL,
    PRIMARY KEY (name)
);

INSERT INTO person (name, birth_date)
    VALUES ('Justin Bieber', FROM_UNIXTIME (762469200));

INSERT INTO person (name, birth_date)
    VALUES ('Elon Musk', FROM_UNIXTIME (46900800));
