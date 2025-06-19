DROP TABLE IF EXISTS cart_item CASCADE;

CREATE TABLE cart_item (
    id SERIAL PRIMARY KEY,
    cart_id INTEGER NOT NULL REFERENCES cart(id),
    product_id INTEGER NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL
);