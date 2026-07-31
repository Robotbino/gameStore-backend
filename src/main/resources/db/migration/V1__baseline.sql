-- ============================================================
-- V1 — Baseline
-- ------------------------------------------------------------
-- Captures the schema exactly as Hibernate's ddl-auto=update
-- left it, so that `ddl-auto=validate` passes unchanged.
--
-- Existing databases are stamped at this version via
-- spring.flyway.baseline-on-migrate=true and never run this
-- script. It exists so a fresh environment (CI, a new laptop,
-- the deployed instance) can build the same schema from zero.
--
-- Quirks preserved deliberately:
--   * games.price is decimal(38,2) — Hibernate's default for an
--     unannotated BigDecimal, not a considered choice.
--   * users.id is int while games.id is bigint. Accepted debt;
--     changing a PK type that other tables reference is real
--     risk for no user-visible benefit.
--   * Hash-named FK constraints are Hibernate's. Kept verbatim
--     so this file is a faithful record; purchases is dropped
--     in V5 regardless.
-- ============================================================

CREATE TABLE `games` (
  `price`       decimal(38,2) NOT NULL,
  `rating`      double DEFAULT NULL,
  `id`          bigint NOT NULL AUTO_INCREMENT,
  `description` text,
  `genre`       varchar(255) DEFAULT NULL,
  `hero_image`  varchar(255) DEFAULT NULL,
  `image_url`   varchar(255) DEFAULT NULL,
  `title`       varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `users` (
  `id`         int NOT NULL AUTO_INCREMENT,
  `is_enabled` bit(1) DEFAULT NULL,
  `points`     int DEFAULT NULL,
  `email`      varchar(255) NOT NULL,
  `password`   varchar(255) NOT NULL,
  `user_name`  varchar(255) NOT NULL,
  `role`       enum('ADMIN','USER') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UKk8d0f2n7n88w1a16yhua64onx` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `purchases` (
  `user_id`       int NOT NULL,
  `game_id`       bigint NOT NULL,
  `id`            bigint NOT NULL AUTO_INCREMENT,
  `purchase_date` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKjrf5tu19isqxe7frhwfp285no` (`game_id`),
  KEY `FKm0ndjymn9p747pfp4515pio8i` (`user_id`),
  CONSTRAINT `FKjrf5tu19isqxe7frhwfp285no` FOREIGN KEY (`game_id`) REFERENCES `games` (`id`),
  CONSTRAINT `FKm0ndjymn9p747pfp4515pio8i` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
