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

create index if not exists idx_orders_order_number_lower
    on orders(lower(order_number));

create index if not exists idx_orders_customer_name_lower
    on orders(lower(customer_name));

create index if not exists idx_orders_customer_phone_lower
    on orders(lower(customer_phone));

create index if not exists idx_orders_customer_email_lower
    on orders(lower(customer_email));
