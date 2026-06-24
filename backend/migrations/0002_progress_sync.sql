CREATE TABLE level_catalog (
    level_id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    difficulty TEXT NOT NULL,
    coin_reward INTEGER NOT NULL CHECK (coin_reward >= 0),
    point_reward INTEGER NOT NULL CHECK (point_reward >= 0),
    content_version INTEGER NOT NULL,
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1))
);

CREATE TABLE completion_rewards (
    uid TEXT NOT NULL,
    level_id TEXT NOT NULL,
    coin_reward INTEGER NOT NULL CHECK (coin_reward >= 0),
    point_reward INTEGER NOT NULL CHECK (point_reward >= 0),
    granted_at INTEGER NOT NULL,
    PRIMARY KEY (uid, level_id),
    FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE
);

CREATE INDEX idx_completion_rewards_uid ON completion_rewards(uid);

INSERT INTO level_catalog
    (level_id, name, difficulty, coin_reward, point_reward, content_version, active)
VALUES
    ('0', 'Euphoria', 'easy', 50, 2, 1, 1),
    ('1', 'Rhythm Factory', 'easy', 50, 2, 1, 1),
    ('2', 'Bounce', 'normal', 75, 3, 1, 1),
    ('3', 'Hypercharge', 'hard', 125, 5, 1, 1),
    ('4', 'Icefield', 'hard', 125, 5, 1, 1),
    ('5', 'Event Horizon', 'insane', 250, 7, 1, 1);
