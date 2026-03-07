ALTER TABLE orders
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS customer_type VARCHAR(32) NOT NULL DEFAULT 'AUTHORIZED',
    ADD COLUMN IF NOT EXISTS customer_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS customer_phone VARCHAR(255),
    ADD COLUMN IF NOT EXISTS customer_email VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_orders_customer_type ON orders (customer_type);
