create table if not exists product_popularity_stats (
    product_id uuid primary key references catalog_products(id) on delete cascade,
    enabled boolean not null default false,
    manual_score integer not null default 0,
    updated_at timestamptz not null
);

create index if not exists idx_product_popularity_stats_enabled_score
    on product_popularity_stats(enabled, manual_score desc);
