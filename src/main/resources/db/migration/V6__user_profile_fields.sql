-- ============================================================
-- V6 — Give users something a profile page can show
-- ------------------------------------------------------------
-- Until now `users` held five columns, three of which (id, role,
-- password) a profile page can't display. There was nothing to
-- render and nothing a signed-in person could edit about
-- themselves — GET /users/me was the whole self-service API.
--
-- These five columns are deliberately FLAT on `users` rather
-- than split into a 1:1 user_profiles table. At this size the
-- split would buy nothing but a join and a lifecycle question
-- (create the row on register, or lazily on first save?), and
-- it would add a box to the ERD that carries no relationship
-- information. Revisit if preferences ever outgrow one row.
--
-- Column notes:
--   * display_name — free-form label, NO unique constraint.
--     user_name stays the unique handle; a person renaming
--     themselves must not be able to collide with someone else,
--     so the editable label and the identity key are separate
--     fields.
--   * avatar_key   — an id into the frontend's preset catalogue,
--     not a URL and not a file. Nothing is uploaded or served;
--     the client renders the mark from the key. An unknown key
--     falls back to the initial circle, so retiring a preset is
--     safe.
--   * bio          — 280 chars. Long enough to say something,
--     short enough that it never needs a TEXT column or its own
--     truncation rules in the UI.
--   * country      — ISO 3166-1 alpha-2. Stored as the code, not
--     the name, so the display language is the client's problem
--     (Intl.DisplayNames) and the stored value never goes stale.
--     varchar(2) rather than the tempting char(2): Hibernate maps
--     a String to VARCHAR, and ddl-auto=validate rejects the
--     mismatch outright. Two wasted bytes beats a schema the app
--     refuses to boot against.
--   * created_at   — NOT NULL, so "member since" always has a
--     value. Existing rows are backfilled below.
--
-- The matching fields land on Users.java in the same commit so
-- ddl-auto=validate passes. Note the H2 test suite runs with
-- flyway.enabled=false and ddl-auto=create-drop, so the suite
-- CANNOT catch drift between this file and the entity — only a
-- real MySQL boot will.
-- ============================================================

-- ── 1. The columns ──────────────────────────────────────────

ALTER TABLE `users`
  ADD COLUMN `display_name` varchar(50)  DEFAULT NULL,
  ADD COLUMN `avatar_key`   varchar(32)  DEFAULT NULL,
  ADD COLUMN `bio`          varchar(280) DEFAULT NULL,
  ADD COLUMN `country`      varchar(2)   DEFAULT NULL,
  ADD COLUMN `created_at`   datetime(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- ── 2. Backfill created_at so "member since" isn't a lie ────
--
-- Every pre-existing row would otherwise read as having joined
-- on the day this migration ran. Their first purchase is the
-- earliest evidence the database actually holds of when they
-- were here, so use it where there is one. Accounts that never
-- bought anything keep the migration timestamp — there is no
-- better answer available, and inventing one would be worse.

UPDATE `users` u
LEFT JOIN (
    SELECT `user_id`, MIN(`purchase_date`) AS `first_purchase`
    FROM `purchases`
    GROUP BY `user_id`
) p ON p.`user_id` = u.`id`
SET u.`created_at` = COALESCE(p.`first_purchase`, u.`created_at`);
