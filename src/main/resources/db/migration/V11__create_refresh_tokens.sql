DROP TABLE IF EXISTS refresh_tokens CASCADE;

CREATE TABLE refresh_tokens (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL UNIQUE REFERENCES users(id),
    expires_at TIMESTAMP NOT NULL,
    hashed_token TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);