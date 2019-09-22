# schema

# --- !Ups
CREATE TABLE `session_storage` (
  `storage_key` VARCHAR(255) NOT NULL,
  `storage_value` TEXT,
  `modified_at` TIMESTAMP,
  PRIMARY KEY (`storage_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE INDEX `idx_session_storage_01` ON `session_storage` (`modified_at`);

CREATE TABLE `verification_storage` (
  `storage_key` VARCHAR(255) NOT NULL,
  `storage_value` TEXT,
  `modified_at` TIMESTAMP,
  PRIMARY KEY (`storage_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE INDEX `idx_verification_storage_01` ON `verification_storage` (`modified_at`);

CREATE TABLE `id_sequence` (
  `sequence_name` VARCHAR(255) NOT NULL UNIQUE,
  `sequence_value` BIGINT NOT NULL,
  PRIMARY KEY (`sequence_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
INSERT INTO `id_sequence` VALUES ('account', 0);

CREATE TABLE `account` (
  `id` BIGINT NOT NULL,
  `email` VARCHAR(255) NOT NULL DEFAULT '' UNIQUE,
  `password_salt` VARCHAR(4) NOT NULL DEFAULT '',
  `password_hash` VARCHAR(40) NOT NULL DEFAULT '',
  `status` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP,
  `updated_at` TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE INDEX `idx_account_01` ON `account` (`email`);

CREATE TABLE `account_access` (
  `code` VARCHAR(255) NOT NULL DEFAULT '',
  `account_id` BIGINT NOT NULL,
  `user_agent` TEXT,
  `remote_address` VARCHAR(40) NOT NULL DEFAULT '',
  `created_at` TIMESTAMP,
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE INDEX `idx_account_access_01` ON `account_access` (`account_id`);

# --- !Downs
DROP TABLE `session_storage`;
DROP TABLE `verification_storage`;
DROP TABLE `id_sequence`;
DROP TABLE `account`;
DROP TABLE `account_access`;
