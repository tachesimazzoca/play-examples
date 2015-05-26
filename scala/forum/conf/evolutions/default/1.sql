# schema

# --- !Ups

--DROP TABLE IF EXISTS `play_evolutions`;

DROP TABLE IF EXISTS `accounts`;
CREATE TABLE `accounts` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(255) NOT NULL DEFAULT '' UNIQUE,
  `password_salt` VARCHAR(4) NOT NULL DEFAULT '',
  `password_hash` VARCHAR(40) NOT NULL DEFAULT '',
  `nickname` TEXT NULL,
  `status` TINYINT(1) NOT NULL DEFAULT 0,
  `modified_at` TIMESTAMP,
  PRIMARY KEY (`id`)
);

DROP TABLE IF EXISTS `session_storage`;
CREATE TABLE `session_storage` (
  `storage_key` VARCHAR(255) NOT NULL DEFAULT '',
  `storage_value` TEXT NULL,
  `storage_timestamp` TIMESTAMP,
  PRIMARY KEY(`storage_key`)
);

# --- !Downs
