CREATE TABLE IF NOT EXISTS `person` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `identifier` varchar(36) NOT NULL,
    `name` varchar(255) NOT NULL,
    `birth_date` timestamp NOT NULL,
    PRIMARY KEY (`id`)
);

INSERT INTO `person` (`identifier`, `name`, `birth_date`)
    VALUES 
    ("247b94f6-34b2-433a-9289-6f3613a11a68", 'Justin Bieber', FROM_UNIXTIME (762469200)),
    ("b4647f79-eb76-4d5d-ac92-31a6161f5dd4", 'Elon Musk', FROM_UNIXTIME (46900800));

