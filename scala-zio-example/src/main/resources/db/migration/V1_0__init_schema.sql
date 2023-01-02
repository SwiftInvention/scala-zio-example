CREATE TABLE IF NOT EXISTS `person` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `identifier` varchar(36) NOT NULL,
    `name` varchar(255) NOT NULL,
    `birth_date` timestamp NOT NULL,
    `gender` int NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE UNIQUE INDEX `person_identifier_uidx` USING BTREE ON `person` (`identifier`);
CREATE INDEX `person_gender_idx` USING BTREE ON `person` (`gender`);
