-- Moves catalog images out of product/category/variant rows into explicit attachment tables.

alter table if exists media_images
    alter column target_id drop not null;

alter table if exists catalog_products
    drop column if exists image_url;

alter table if exists catalog_product_variants
    drop column if exists image_url;

alter table if exists catalog_categories
    drop column if exists image_url;

create table if not exists catalog_product_images (
    id uuid primary key,
    product_id uuid not null references catalog_products(id) on delete cascade,
    image_id uuid not null references media_images(id) on delete cascade,
    sort_order integer not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_catalog_product_images_owner_image unique (product_id, image_id)
);

create index if not exists idx_catalog_product_images_product
    on catalog_product_images(product_id);

create index if not exists idx_catalog_product_images_image
    on catalog_product_images(image_id);

create table if not exists catalog_product_variant_images (
    id uuid primary key,
    variant_id uuid not null references catalog_product_variants(id) on delete cascade,
    image_id uuid not null references media_images(id) on delete cascade,
    sort_order integer not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_catalog_product_variant_images_owner_image unique (variant_id, image_id)
);

create index if not exists idx_catalog_product_variant_images_variant
    on catalog_product_variant_images(variant_id);

create index if not exists idx_catalog_product_variant_images_image
    on catalog_product_variant_images(image_id);

create table if not exists catalog_category_images (
    id uuid primary key,
    category_id uuid not null references catalog_categories(id) on delete cascade,
    image_id uuid not null references media_images(id) on delete cascade,
    sort_order integer not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_catalog_category_images_owner_image unique (category_id, image_id)
);

create index if not exists idx_catalog_category_images_category
    on catalog_category_images(category_id);

create index if not exists idx_catalog_category_images_image
    on catalog_category_images(image_id);
