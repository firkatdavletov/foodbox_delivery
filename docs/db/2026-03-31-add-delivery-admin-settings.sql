create table if not exists delivery_method_settings (
    method varchar(32) primary key,
    is_enabled boolean not null,
    sort_order integer not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table if not exists checkout_payment_method_rules (
    id uuid primary key,
    delivery_method varchar(32) not null,
    payment_method_code varchar(32) not null,
    sort_order integer not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_checkout_payment_method_rules_delivery_method_payment_method
        unique (delivery_method, payment_method_code)
);
