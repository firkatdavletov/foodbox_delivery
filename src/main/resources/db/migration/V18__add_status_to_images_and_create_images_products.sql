ALTER TABLE images
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'UPLOADING';

DROP TABLE IF EXISTS images_products CASCADE;

CREATE TABLE images_products (
    product_id INTEGER NOT NULL REFERENCES products(id),
    image_id INTEGER NOT NULL REFERENCES images(id),
    PRIMARY KEY (product_id, image_id)
);