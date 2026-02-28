DROP TABLE IF EXISTS products_categories CASCADE;

CREATE TABLE products_categories (
    product_id INTEGER NOT NULL REFERENCES products(id),
    category_id INTEGER NOT NULL REFERENCES categories(id),
    PRIMARY KEY (product_id, category_id)
);

INSERT INTO products_categories (product_id, category_id)
SELECT id, category_id
FROM products
WHERE category_id IS NOT NULL
ON CONFLICT DO NOTHING;

-- products_categories
create index idx_products_categories_product on products_categories(product_id);
create index idx_products_categories_category on products_categories(category_id);