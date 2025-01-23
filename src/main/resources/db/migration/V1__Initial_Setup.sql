-- CREATE DATABASE IF NOT EXISTS `upay` /*!40100 DEFAULT CHARACTER SET utf8 */;

-- USE `upay`;

CREATE TABLE IF NOT EXISTS wrb_wallet (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uid` VARCHAR(64) NOT NULL,
  `address` VARCHAR(64) NOT NULL,
  `ecryped_private_key` VARCHAR(1024) NOT NULL, -- encrypted private key of wallet by random aes key
  `encrypted_aes_key` VARCHAR(1024) NOT NULL, -- encrypted aes key by system public key
  `chain_type` smallint(1) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE UNIQUE INDEX `u_address` ON wrb_wallet (address);
CREATE UNIQUE INDEX `idx_uid_chain_type` ON wrb_wallet (uid, chain_type);


CREATE TABLE IF NOT EXISTS wrb_notify_task (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `url` VARCHAR(128) NOT NULL,
  `data` VARCHAR(256)  NULL,
  `payment_id` VARCHAR(64) NOT NULL,
  `oid` VARCHAR(64) NOT NULL,
  `uid` VARCHAR(64) NOT NULL,
  `retry_count` int(11) NOT NULL DEFAULT 0,
  `status` smallint(1) NOT NULL,
  `next_notify_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `executed_at` DATETIME,
  `executor` VARCHAR(64) NULL,
  PRIMARY KEY (id),
  INDEX `idx_uid` (`uid`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE UNIQUE INDEX `u_payment_id` ON wrb_notify_task (payment_id);
CREATE UNIQUE INDEX `u_oid` ON wrb_notify_task (oid);

CREATE TABLE IF NOT EXISTS wrb_payment_order (
  `id` VARCHAR(32) NOT NULL,
  `oid` VARCHAR(64) NOT NULL,
  `uid` VARCHAR(64) NOT NULL,
  `amount` DECIMAL(10, 4) NOT NULL,
  `status` smallint(1) NOT NULL,
  `expired_at` DATETIME NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `memo` VARCHAR(128) NOT NULL,
  `paid_at` DATETIME,
  `notify_url` VARCHAR(128) NOT NULL,
  `redirect_url` VARCHAR(128) NOT NULL,
  `mch_id` VARCHAR(64) NOT NULL,
  `closed_at` DATETIME,
  `locked_address` VARCHAR(64) NULL,
  `logo` VARCHAR(128) NULL,
  PRIMARY KEY (id),
  INDEX `idx_uid` (`uid`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE UNIQUE INDEX `u_oid_mch` ON wrb_payment_order (oid, mch_id);

CREATE TABLE IF NOT EXISTS wrb_trade_log (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `payment_id` VARCHAR(64),
  `uid` VARCHAR(64) NOT NULL,
  `amount` DECIMAL(10, 4) NOT NULL,
  `type` smallint(1) NOT NULL,
  `confirmed_blocks` int(11) NOT NULL DEFAULT 0,
  `memo` VARCHAR(128) NULL,
  `tx_hash` VARCHAR(128) NULL,
  `token` VARCHAR(64) NULL,
  `chain_type` smallint(1),
  `chain_id` INT(11) NULL,
  `tx_from` VARCHAR(64) NULL,
  `tx_to` VARCHAR(64) NULL,
  `block_number` VARCHAR(20),
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  INDEX `idx_uid` (`uid`),
  INDEX `idx_payment_id` (`payment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;