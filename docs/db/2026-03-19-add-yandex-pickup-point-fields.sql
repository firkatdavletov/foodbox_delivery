ALTER TABLE cart_delivery_drafts
    ADD COLUMN pickup_point_external_id VARCHAR(64),
    ADD COLUMN pickup_point_name VARCHAR(255),
    ADD COLUMN pickup_point_address VARCHAR(500);

ALTER TABLE order_delivery_snapshots
    ADD COLUMN pickup_point_external_id VARCHAR(64);
