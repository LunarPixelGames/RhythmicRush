PRAGMA foreign_keys = ON;

CREATE TABLE users (
    uid TEXT PRIMARY KEY NOT NULL,
    username TEXT,
    email_verified INTEGER NOT NULL DEFAULT 0 CHECK (email_verified IN (0, 1)),
    providers_json TEXT NOT NULL DEFAULT '[]',
    status TEXT NOT NULL DEFAULT 'active'
        CHECK (status IN ('active', 'merging', 'suspended', 'deleted')),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    deleted_at INTEGER
);

CREATE TABLE usernames (
    normalized TEXT PRIMARY KEY NOT NULL,
    display_name TEXT NOT NULL,
    uid TEXT NOT NULL UNIQUE,
    reserved_at INTEGER NOT NULL,
    released_at INTEGER
);

CREATE TABLE saves (
    uid TEXT PRIMARY KEY NOT NULL,
    schema_version INTEGER NOT NULL DEFAULT 1,
    content_version INTEGER NOT NULL DEFAULT 1,
    revision INTEGER NOT NULL DEFAULT 0,
    legacy_coin_floor INTEGER NOT NULL DEFAULT 0,
    earned_coins INTEGER NOT NULL DEFAULT 0,
    points INTEGER NOT NULL DEFAULT 0,
    completed_levels INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE
);

CREATE TABLE level_progress (
    uid TEXT NOT NULL,
    level_id TEXT NOT NULL,
    best_percent INTEGER NOT NULL DEFAULT 0 CHECK (best_percent BETWEEN 0 AND 100),
    completed INTEGER NOT NULL DEFAULT 0 CHECK (completed IN (0, 1)),
    completion_reward_granted INTEGER NOT NULL DEFAULT 0
        CHECK (completion_reward_granted IN (0, 1)),
    completed_at INTEGER,
    updated_at INTEGER NOT NULL,
    PRIMARY KEY (uid, level_id),
    FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE
);

CREATE TABLE device_attempts (
    uid TEXT NOT NULL,
    device_id TEXT NOT NULL,
    level_id TEXT NOT NULL,
    accepted_total INTEGER NOT NULL DEFAULT 0 CHECK (accepted_total >= 0),
    updated_at INTEGER NOT NULL,
    PRIMARY KEY (uid, device_id, level_id),
    FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE
);

CREATE TABLE operations (
    uid TEXT NOT NULL,
    operation_key TEXT NOT NULL,
    operation_type TEXT NOT NULL,
    request_hash TEXT NOT NULL,
    status_code INTEGER NOT NULL,
    response_json TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    PRIMARY KEY (uid, operation_key),
    FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE
);

CREATE TABLE leaderboard_entries (
    uid TEXT PRIMARY KEY NOT NULL,
    username TEXT NOT NULL,
    points INTEGER NOT NULL DEFAULT 0,
    completed_levels INTEGER NOT NULL DEFAULT 0,
    score_achieved_at INTEGER NOT NULL,
    eligible INTEGER NOT NULL DEFAULT 0 CHECK (eligible IN (0, 1)),
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE
);

CREATE TABLE leaderboard_cache (
    cache_key TEXT PRIMARY KEY NOT NULL,
    payload_json TEXT NOT NULL,
    generated_at INTEGER NOT NULL,
    lease_until INTEGER
);

CREATE TABLE merge_tickets (
    ticket_id TEXT PRIMARY KEY NOT NULL,
    canonical_uid TEXT NOT NULL,
    secondary_uid TEXT NOT NULL,
    state TEXT NOT NULL,
    preview_json TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL
);

CREATE TABLE rate_limits (
    subject TEXT NOT NULL,
    action TEXT NOT NULL,
    window_started_at INTEGER NOT NULL,
    count INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (subject, action)
);

CREATE TABLE account_tombstones (
    uid TEXT PRIMARY KEY NOT NULL,
    deleted_at INTEGER NOT NULL,
    reason TEXT NOT NULL DEFAULT 'user_request'
);

CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_level_progress_uid ON level_progress(uid);
CREATE INDEX idx_device_attempts_uid ON device_attempts(uid);
CREATE INDEX idx_operations_expiry ON operations(expires_at);
CREATE INDEX idx_leaderboard_rank
    ON leaderboard_entries(eligible, points DESC, completed_levels DESC, score_achieved_at ASC);
CREATE INDEX idx_merge_canonical ON merge_tickets(canonical_uid, state);
CREATE INDEX idx_merge_secondary ON merge_tickets(secondary_uid, state);
CREATE INDEX idx_account_tombstones_deleted_at ON account_tombstones(deleted_at);
