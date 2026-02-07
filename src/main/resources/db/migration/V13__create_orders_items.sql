DROP TABLE IF EXISTS order_items CASCADE;

CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders(id),
    product_id INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    price REAL NOT NULL
);

-- order_items
create index idx_order_items_order on order_items(order_id);