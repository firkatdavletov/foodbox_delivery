DROP TABLE IF EXISTS products CASCADE;

CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    price DOUBLE PRECISION NOT NULL,
    image_url VARCHAR(255),
    category_id INTEGER NOT NULL
);