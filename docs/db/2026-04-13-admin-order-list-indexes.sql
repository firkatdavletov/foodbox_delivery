create extension if not exists pg_trgm;

create index if not exists idx_orders_created_at
    on orders(created_at);

create index if not exists idx_orders_customer_name
    on orders(customer_name);

create index if not exists idx_orders_customer_phone
    on orders(customer_phone);

create index if not exists idx_orders_customer_email
    on orders(customer_email);

create index if not exists idx_order_delivery_snapshots_delivery_method
    on order_delivery_snapshots(delivery_method);

create index if not exists idx_orders_order_number_trgm
    on orders using gin (lower(order_number) gin_trgm_ops);

create index if not exists idx_orders_customer_name_trgm
    on orders using gin (lower(customer_name) gin_trgm_ops);

create index if not exists idx_orders_customer_phone_trgm
    on orders using gin (lower(customer_phone) gin_trgm_ops);

create index if not exists idx_orders_customer_email_trgm
    on orders using gin (lower(customer_email) gin_trgm_ops);

create index if not exists idx_payments_status_paid_at_order_id
    on payments(status, paid_at, order_id);

create index if not exists idx_payments_order_id_created_at_desc
    on payments(order_id, created_at desc);

create index if not exists idx_carts_status
    on carts(status);

create index if not exists idx_catalog_product_variants_product_id
    on catalog_product_variants(product_id);
