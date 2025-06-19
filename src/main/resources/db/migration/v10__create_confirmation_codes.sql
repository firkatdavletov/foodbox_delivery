DROP TABLE IF EXISTS confirmation_codes CASCADE;

CREATE TABLE confirmation_codes (
    id SERIAL PRIMARY KEY,
    phone VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
);