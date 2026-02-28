ALTER TABLE products
    ADD COLUMN IF NOT EXISTS sku VARCHAR(255);

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS sku VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS idx_products_sku ON products (sku);
CREATE UNIQUE INDEX IF NOT EXISTS idx_categories_sku ON categories (sku);
