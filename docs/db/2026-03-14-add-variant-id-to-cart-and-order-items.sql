-- Supports product variants in cart and order items.
ALTER TABLE cart_items
    ADD COLUMN IF NOT EXISTS variant_id uuid;

ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS variant_id uuid;
