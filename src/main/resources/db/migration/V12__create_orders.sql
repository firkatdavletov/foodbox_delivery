DROP TABLE IF EXISTS orders CASCADE;

CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id),
    cart_id INTEGER NOT NULL REFERENCES cart(id),
    status VARCHAR(255) NOT NULL,
    paid_at TIMESTAMP,
    delivery_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0
);

-- orders
create index idx_orders_user on orders(user_id);
create index idx_orders_status on orders(status);