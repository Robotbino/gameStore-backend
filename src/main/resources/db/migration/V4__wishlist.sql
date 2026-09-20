-- ============================================================
-- V4 — Wishlist
-- ------------------------------------------------------------
-- One row per (user, game) a user has saved for later. Mirrors
-- the `purchases` shape and column types (users.id is int,
-- games.id is bigint). The unique key makes "wishlist the same
-- game twice" impossible at the storage level; the service
-- treats a repeat as a no-op before it ever gets here.
--
-- Rows cascade away with their owner or their game so deleting
-- either never trips on a wishlist FK.
-- ============================================================
CREATE TABLE `wishlist` (
  `id`       bigint      NOT NULL AUTO_INCREMENT,
  `user_id`  int         NOT NULL,
  `game_id`  bigint      NOT NULL,
  `added_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wishlist_user_game` (`user_id`, `game_id`),
  KEY `idx_wishlist_game` (`game_id`),
  CONSTRAINT `fk_wishlist_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_wishlist_game` FOREIGN KEY (`game_id`) REFERENCES `games` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
