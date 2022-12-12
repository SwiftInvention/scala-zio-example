CREATE TABLE IF NOT EXISTS `person` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `identifier` varchar(36) NOT NULL,
    `name` varchar(255) NOT NULL,
    `birth_date` timestamp NOT NULL,
    `gender` INT NOT NULL,
    PRIMARY KEY (`id`)
);