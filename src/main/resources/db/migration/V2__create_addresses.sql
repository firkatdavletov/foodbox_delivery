DROP TABLE IF EXISTS addresses CASCADE;

CREATE TABLE addresses (
    id SERIAL PRIMARY KEY,
    lat DOUBLE PRECISION NOT NULL,
    long DOUBLE PRECISION NOT NULL,
    city VARCHAR(255) NOT NULL,
    street VARCHAR(255) NOT NULL,
    house VARCHAR(255) NOT NULL,
    flat VARCHAR(255),
    intercome VARCHAR(255),
    comment TEXT,
    user_id INTEGER REFERENCES users(id)
);
