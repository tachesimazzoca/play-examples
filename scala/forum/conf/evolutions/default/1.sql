# schema

# --- !Ups

DROP TABLE IF EXISTS `play_evolutions`;

DROP TABLE IF EXISTS `accounts`;
CREATE TABLE `accounts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `password_salt` varchar(255) NOT NULL,
  `active` tinyint(1) NOT NULL default '0',
  `modified_at` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `signup_storage`;
CREATE TABLE `signup_storage` (
  `storage_key` varchar(255) binary NOT NULL,
  `storage_value` text NULL,
  `storage_timestamp` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  KEY (`storage_timestamp`),
  KEY (`storage_timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# --- !Downs
