alter table if exists carts
    add column if not exists promo_code varchar(64);

alter table if exists carts
    add column if not exists promo_discount_minor bigint not null default 0;

create index if not exists idx_carts_promo_code
    on carts(promo_code);
