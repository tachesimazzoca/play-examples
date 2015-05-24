# schema

# --- !Ups

--DROP TABLE IF EXISTS `play_evolutions`;

DROP TABLE IF EXISTS `accounts`;
CREATE TABLE `accounts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `password_salt` varchar(255) NOT NULL,
  `active` boolean NOT NULL,
  `modified_at` timestamp,
  PRIMARY KEY (`id`)
);

DROP TABLE IF EXISTS `session_storage`;
CREATE TABLE `session_storage` (
  `storage_key` varchar(255) NOT NULL,
  `storage_value` text NULL,
  `storage_timestamp` timestamp,
  PRIMARY KEY(`storage_key`)
);

# --- !Downs
