create table if not exists delivery_offers (
    id uuid primary key,
    provider varchar(32) not null,
    external_offer_id varchar(128) not null unique,
    expires_at timestamp with time zone null,
    pricing_minor bigint null,
    pricing_total_minor bigint null,
    currency varchar(3) null,
    commission_on_delivery_percent varchar(32) null,
    commission_on_delivery_amount_minor bigint null,
    delivery_policy varchar(64) null,
    delivery_interval_from timestamp with time zone null,
    delivery_interval_to timestamp with time zone null,
    pickup_interval_from timestamp with time zone null,
    pickup_interval_to timestamp with time zone null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table if not exists order_delivery_offers (
    id uuid primary key,
    order_id uuid not null unique references orders(id) on delete cascade,
    offer_id uuid not null unique references delivery_offers(id) on delete cascade,
    external_request_id varchar(128) null,
    confirmed_at timestamp with time zone null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);
