-- ============================================================
-- V5 — Orders + rewards (roadmap B17)
-- ------------------------------------------------------------
-- The economy is simulated: no real money ever moves. `purchases`
-- stays the ownership table (what the library shows); `orders`
-- is the receipt — totals, the payment method chosen, and a
-- DEMO- reference. `order_items` snapshots title and price at the
-- moment of purchase so a later price change never rewrites
-- history; game_id is SET NULL on delete for the same reason.
--
-- `reward_transactions` is the points ledger. users.points stays
-- the denormalised balance and is updated in the same transaction
-- as each ledger row (balance_after doubles as an audit trail).
--
-- Column types mirror the existing tables: users.id is int,
-- games.id is bigint.
-- ============================================================
CREATE TABLE `orders` (
  `id`                bigint        NOT NULL AUTO_INCREMENT,
  `user_id`           int           NOT NULL,
  `subtotal`          decimal(10,2) NOT NULL,
  `points_redeemed`   int           NOT NULL DEFAULT 0,
  `discount`          decimal(10,2) NOT NULL DEFAULT 0.00,
  `total`             decimal(10,2) NOT NULL,
  `points_earned`     int           NOT NULL DEFAULT 0,
  `payment_method`    varchar(20)   NOT NULL,
  `payment_reference` varchar(32)   NOT NULL,
  `status`            varchar(20)   NOT NULL,
  `created_at`        datetime(6)   NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_orders_reference` (`payment_reference`),
  KEY `idx_orders_user` (`user_id`),
  CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `order_items` (
  `id`         bigint        NOT NULL AUTO_INCREMENT,
  `order_id`   bigint        NOT NULL,
  `game_id`    bigint        NULL,
  `title`      varchar(255)  NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order_items_order` (`order_id`),
  KEY `idx_order_items_game` (`game_id`),
  CONSTRAINT `fk_order_items_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_order_items_game`  FOREIGN KEY (`game_id`)  REFERENCES `games` (`id`)  ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reward_transactions` (
  `id`            bigint      NOT NULL AUTO_INCREMENT,
  `user_id`       int         NOT NULL,
  `order_id`      bigint      NULL,
  `delta`         int         NOT NULL,
  `balance_after` int         NOT NULL,
  `reason`        varchar(20) NOT NULL,
  `created_at`    datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_reward_tx_user` (`user_id`),
  KEY `idx_reward_tx_order` (`order_id`),
  CONSTRAINT `fk_reward_tx_user`  FOREIGN KEY (`user_id`)  REFERENCES `users` (`id`)  ON DELETE CASCADE,
  CONSTRAINT `fk_reward_tx_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- V1 declared users.points as `int DEFAULT NULL`; rows created before
-- the entity default existed may still hold NULL. Points arithmetic
-- starts from zero.
UPDATE `users` SET `points` = 0 WHERE `points` IS NULL;
