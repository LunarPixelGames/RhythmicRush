CREATE TABLE deletion_requests (
    uid TEXT PRIMARY KEY NOT NULL,
    requested_at INTEGER NOT NULL,
    delete_after INTEGER NOT NULL,
    recovery_token_hash TEXT NOT NULL,
    recovery_email TEXT NOT NULL,
    recovery_url TEXT NOT NULL,
    delivery_mode TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    recovered_at INTEGER,
    finalized_at INTEGER,
    FOREIGN KEY (uid) REFERENCES users(uid) ON DELETE CASCADE
);

CREATE INDEX idx_deletion_requests_due
    ON deletion_requests(delete_after, finalized_at, recovered_at);

CREATE INDEX idx_deletion_requests_token
    ON deletion_requests(recovery_token_hash);

DELETE FROM leaderboard_cache WHERE cache_key = 'global-points-v1';
