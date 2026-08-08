-- ============================================================
-- V2 — Deduplicate the catalogue and standardise its artwork
-- ------------------------------------------------------------
-- Three seed runs stacked on top of each other, so `games` held
-- 78 rows for 30 real titles: the original nine seeded twice
-- (ids 1-9, 10-18) with hand-picked Google/Wikipedia art, then
-- the full thirty-title Steam-sourced set seeded twice
-- (ids 19-48, 49-78). Duplicates were not caught because two
-- rows for the same game carried different image URLs, and
-- nothing constrained `title`.
--
-- This migration:
--   1. folds the title variants together so the dedupe key is exact;
--   2. re-points `purchases` at the surviving row before deleting;
--   3. keeps the lowest id per title, deletes the rest;
--   4. repoints every Steam title at its official CDN artwork —
--      library_600x900 (2:3 portrait) for the grid, library_hero
--      (wide) for the banner;
--   5. adds UNIQUE(title) so a re-run of any seeder fails loudly
--      instead of silently tripling the catalogue again.
--
-- Note on the two art sizes: `image_url` and `hero_image` are not
-- interchangeable. `image_url` feeds the portrait card grid and the
-- square sidebar/cart thumbnails; `hero_image` feeds the 21:9
-- banners. Filling `image_url` with a landscape header.jpg is what
-- cropped the characters out of the grid — the fix is a genuinely
-- portrait source, not a CSS crop.
--
-- Minecraft is the one exception: it has never shipped on Steam, so
-- there is no library_600x900 for it. It keeps Mojang's official
-- 864x864 key art, the only square-or-taller official asset that
-- resolves. Its previous hero_image (an MC-Legends background) now
-- 301-redirects and is replaced.
-- ============================================================

-- ── 1. Fold title variants ──────────────────────────────────
-- The 2021-era rows spelled this without the space after the colon.
-- Steam's own store page uses "NARAKA: BLADEPOINT".
UPDATE `games`
SET `title` = 'NARAKA: BLADEPOINT'
WHERE `title` = 'NARAKA:BLADEPOINT';


-- ── 2. Map every duplicate onto its surviving row ───────────
-- Materialised into a temp table because MySQL refuses a DELETE
-- whose subquery selects from the table being deleted from (1093).
CREATE TEMPORARY TABLE `games_dupe_map` (
  `dup_id`       bigint NOT NULL PRIMARY KEY,
  `canonical_id` bigint NOT NULL
) ENGINE=InnoDB;

INSERT INTO `games_dupe_map` (`dup_id`, `canonical_id`)
SELECT g.`id`, c.`canonical_id`
FROM `games` g
JOIN (
  SELECT `title`, MIN(`id`) AS `canonical_id`
  FROM `games`
  GROUP BY `title`
) c ON c.`title` = g.`title`
WHERE g.`id` <> c.`canonical_id`;


-- ── 3. Preserve purchase history, then delete ───────────────
-- A user who bought "Elden Ring" bought the game, not the row. Move
-- their purchase to the surviving id first; deleting a referenced
-- row would otherwise fail on the games FK.
UPDATE `purchases` p
JOIN `games_dupe_map` m ON m.`dup_id` = p.`game_id`
SET p.`game_id` = m.`canonical_id`;

DELETE g
FROM `games` g
JOIN `games_dupe_map` m ON m.`dup_id` = g.`id`;

DROP TEMPORARY TABLE `games_dupe_map`;

-- The re-point above can leave a user holding two purchases of what is now
-- the same game — one bought off each duplicate listing. PurchaseService
-- treats (user, game) as unique (existsByUserAndGame), so leaving both would
-- show one title twice in that user's library. Keep the first row inserted.
CREATE TEMPORARY TABLE `purchase_keepers` (
  `id` bigint NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO `purchase_keepers` (`id`)
SELECT MIN(`id`) FROM `purchases` GROUP BY `user_id`, `game_id`;

DELETE p
FROM `purchases` p
LEFT JOIN `purchase_keepers` k ON k.`id` = p.`id`
WHERE k.`id` IS NULL;

DROP TEMPORARY TABLE `purchase_keepers`;


-- ── 4. Standardise artwork on Steam's official CDN ──────────
CREATE TEMPORARY TABLE `steam_appids` (
  `title` varchar(255) NOT NULL PRIMARY KEY,
  `appid` varchar(16)  NOT NULL
) ENGINE=InnoDB;

INSERT INTO `steam_appids` (`title`, `appid`) VALUES
  ('The Witcher 3',                  '292030'),
  ('Cyberpunk 2077',                 '1091500'),
  ('Among Us',                       '945360'),
  ('Hades',                          '1145360'),
  ('ARC Raiders',                    '2183900'),
  ('Dota 2',                         '570'),
  ('Stardew Valley',                 '413150'),
  ('NARAKA: BLADEPOINT',             '1203220'),
  ('Elden Ring',                     '1245620'),
  ('Red Dead Redemption 2',          '1174180'),
  ('Grand Theft Auto V',             '271590'),
  ('Counter-Strike 2',               '730'),
  ('Terraria',                       '105600'),
  ('Portal 2',                       '620'),
  ('Half-Life: Alyx',                '546560'),
  ('Sekiro: Shadows Die Twice',      '814380'),
  ('Dark Souls III',                 '374320'),
  ('DOOM Eternal',                   '782330'),
  ('Hollow Knight',                  '367520'),
  ('Celeste',                        '504230'),
  ('Baldur''s Gate 3',               '1086940'),
  ('God of War',                     '1593500'),
  ('Marvel''s Spider-Man Remastered','1817070'),
  ('Death Stranding',                '1190460'),
  ('Resident Evil 4',                '2050650'),
  ('Monster Hunter: World',          '582010'),
  ('Rust',                           '252490'),
  ('Valheim',                        '892970'),
  ('Left 4 Dead 2',                  '550');

UPDATE `games` g
JOIN `steam_appids` s ON s.`title` = g.`title`
SET
  g.`image_url`  = CONCAT('https://cdn.cloudflare.steamstatic.com/steam/apps/', s.`appid`, '/library_600x900.jpg'),
  g.`hero_image` = CONCAT('https://cdn.cloudflare.steamstatic.com/steam/apps/', s.`appid`, '/library_hero.jpg');

DROP TEMPORARY TABLE `steam_appids`;

-- Minecraft — not on Steam. Mojang's 864x864 key art is square rather
-- than 2:3, but it is centre-composed, so the portrait card crops the
-- sides without touching the characters.
UPDATE `games`
SET
  `image_url`  = 'https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/key-art/Homepage_Discover-our-games_MC-Vanilla-KeyArt_864x864.jpg',
  `hero_image` = 'https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/key-art/Homepage_Discover-our-games_MC-Vanilla-KeyArt_864x864.jpg'
WHERE `title` = 'Minecraft';


-- ── 5. Stop it happening again ──────────────────────────────
-- The catalogue has one row per title by definition. Making that a
-- database constraint turns a silent re-seed into a visible error,
-- and gives GamesService a real signal to raise
-- DuplicateResourceException on.
ALTER TABLE `games`
  ADD UNIQUE KEY `uk_games_title` (`title`);
