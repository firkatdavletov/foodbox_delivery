create table if not exists promo_codes (
    id uuid primary key,
    code varchar(64) not null unique,
    discount_type varchar(32) not null,
    discount_value bigint not null,
    min_order_amount_minor bigint null,
    max_discount_minor bigint null,
    currency varchar(3) null,
    starts_at timestamp with time zone null,
    ends_at timestamp with time zone null,
    usage_limit_total integer null,
    usage_limit_per_user integer null,
    used_count integer not null default 0,
    active boolean not null default true,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index if not exists idx_promo_codes_code
    on promo_codes(code);

create index if not exists idx_promo_codes_active
    on promo_codes(active);

create table if not exists promo_code_redemptions (
    id uuid primary key,
    promo_code_id uuid not null references promo_codes(id) on delete cascade,
    order_id uuid not null unique references orders(id) on delete cascade,
    user_id uuid null,
    discount_minor bigint not null,
    created_at timestamp with time zone not null
);

create index if not exists idx_promo_code_redemptions_promo_code_id
    on promo_code_redemptions(promo_code_id);

create index if not exists idx_promo_code_redemptions_user_id
    on promo_code_redemptions(user_id);

create table if not exists gift_certificates (
    id uuid primary key,
    code varchar(64) not null unique,
    initial_amount_minor bigint not null,
    balance_minor bigint not null,
    currency varchar(3) not null,
    status varchar(32) not null,
    expires_at timestamp with time zone null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index if not exists idx_gift_certificates_code
    on gift_certificates(code);

create index if not exists idx_gift_certificates_status
    on gift_certificates(status);

create table if not exists gift_certificate_transactions (
    id uuid primary key,
    gift_certificate_id uuid not null references gift_certificates(id) on delete cascade,
    order_id uuid not null references orders(id) on delete cascade,
    type varchar(16) not null,
    amount_minor bigint not null,
    created_at timestamp with time zone not null
);

create index if not exists idx_gift_certificate_tx_certificate_id
    on gift_certificate_transactions(gift_certificate_id);

create index if not exists idx_gift_certificate_tx_order_id
    on gift_certificate_transactions(order_id);

alter table if exists orders
    add column if not exists promo_code varchar(64);

alter table if exists orders
    add column if not exists promo_discount_minor bigint not null default 0;

alter table if exists orders
    add column if not exists gift_certificate_id uuid;

alter table if exists orders
    add column if not exists gift_certificate_code_last4 varchar(4);

alter table if exists orders
    add column if not exists gift_certificate_amount_minor bigint not null default 0;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'fk_orders_gift_certificate_id'
    ) then
        alter table orders
            add constraint fk_orders_gift_certificate_id
                foreign key (gift_certificate_id) references gift_certificates(id);
    end if;
end $$;

create index if not exists idx_orders_promo_code
    on orders(promo_code);

create index if not exists idx_orders_gift_certificate_id
    on orders(gift_certificate_id);
