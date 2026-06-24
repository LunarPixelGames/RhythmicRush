ALTER TABLE users
ADD COLUMN leaderboard_banned INTEGER NOT NULL DEFAULT 0
    CHECK (leaderboard_banned IN (0, 1));

ALTER TABLE users
ADD COLUMN leaderboard_ban_reason TEXT;

CREATE INDEX idx_users_leaderboard_banned
    ON users(leaderboard_banned, status);

DELETE FROM leaderboard_cache WHERE cache_key = 'global-points-v1';
