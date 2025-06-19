DROP TABLE IF EXISTS categories CASCADE;

CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    parent_category_id INTEGER,
    image_url VARCHAR(255)
);