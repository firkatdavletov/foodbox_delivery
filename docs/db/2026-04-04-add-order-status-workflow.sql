create table if not exists order_status_definitions (
    id uuid primary key,
    code varchar(64) not null,
    name varchar(255) not null,
    description text null,
    state_type varchar(64) not null,
    color varchar(32) null,
    icon varchar(64) null,
    is_initial boolean not null,
    is_final boolean not null,
    is_cancellable boolean not null,
    is_active boolean not null,
    visible_to_customer boolean not null,
    notify_customer boolean not null,
    notify_staff boolean not null,
    sort_order integer not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_order_status_definitions_code unique (code)
);

create index if not exists idx_order_status_definitions_state_type
    on order_status_definitions(state_type);

create index if not exists idx_order_status_definitions_sort_order
    on order_status_definitions(sort_order);

create table if not exists order_status_transitions (
    id uuid primary key,
    from_status_id uuid not null references order_status_definitions(id) on delete cascade,
    to_status_id uuid not null references order_status_definitions(id) on delete cascade,
    required_role varchar(32) null,
    is_automatic boolean not null,
    guard_code varchar(64) null,
    is_active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_order_status_transitions_from_to unique (from_status_id, to_status_id)
);

create index if not exists idx_order_status_transitions_from_status_id
    on order_status_transitions(from_status_id);

create index if not exists idx_order_status_transitions_to_status_id
    on order_status_transitions(to_status_id);

alter table if exists orders
    add column if not exists current_status_id uuid;

alter table if exists orders
    add column if not exists status_changed_at timestamp with time zone;

create index if not exists idx_orders_current_status_id
    on orders(current_status_id);

insert into order_status_definitions (
    id,
    code,
    name,
    description,
    state_type,
    color,
    icon,
    is_initial,
    is_final,
    is_cancellable,
    is_active,
    visible_to_customer,
    notify_customer,
    notify_staff,
    sort_order,
    created_at,
    updated_at
)
values
    (
        '00000000-0000-0000-0000-000000000101',
        'PENDING',
        'Pending',
        'New order awaiting confirmation',
        'AWAITING_CONFIRMATION',
        '#F59E0B',
        'clock',
        true,
        false,
        true,
        true,
        true,
        false,
        true,
        10,
        now(),
        now()
    ),
    (
        '00000000-0000-0000-0000-000000000102',
        'CONFIRMED',
        'Confirmed',
        'Order confirmed and accepted into work',
        'CONFIRMED',
        '#2563EB',
        'check-circle',
        false,
        false,
        true,
        true,
        true,
        true,
        true,
        20,
        now(),
        now()
    ),
    (
        '00000000-0000-0000-0000-000000000103',
        'CANCELLED',
        'Cancelled',
        'Order was cancelled',
        'CANCELED',
        '#DC2626',
        'x-circle',
        false,
        true,
        false,
        true,
        true,
        true,
        true,
        90,
        now(),
        now()
    ),
    (
        '00000000-0000-0000-0000-000000000104',
        'COMPLETED',
        'Completed',
        'Order completed successfully',
        'COMPLETED',
        '#16A34A',
        'check-badge',
        false,
        true,
        false,
        true,
        true,
        true,
        true,
        100,
        now(),
        now()
    )
on conflict (code) do nothing;

insert into order_status_transitions (
    id,
    from_status_id,
    to_status_id,
    required_role,
    is_automatic,
    guard_code,
    is_active,
    created_at,
    updated_at
)
values
    (
        '00000000-0000-0000-0000-000000000201',
        '00000000-0000-0000-0000-000000000101',
        '00000000-0000-0000-0000-000000000102',
        null,
        true,
        null,
        true,
        now(),
        now()
    ),
    (
        '00000000-0000-0000-0000-000000000202',
        '00000000-0000-0000-0000-000000000101',
        '00000000-0000-0000-0000-000000000103',
        null,
        false,
        null,
        true,
        now(),
        now()
    ),
    (
        '00000000-0000-0000-0000-000000000203',
        '00000000-0000-0000-0000-000000000102',
        '00000000-0000-0000-0000-000000000104',
        null,
        false,
        null,
        true,
        now(),
        now()
    ),
    (
        '00000000-0000-0000-0000-000000000204',
        '00000000-0000-0000-0000-000000000102',
        '00000000-0000-0000-0000-000000000103',
        null,
        false,
        null,
        true,
        now(),
        now()
    )
on conflict (from_status_id, to_status_id) do nothing;

do $$
begin
    if exists (
        select 1
        from information_schema.columns
        where table_name = 'orders'
          and column_name = 'status'
    ) then
        update orders
        set current_status_id = case status
            when 'PENDING' then '00000000-0000-0000-0000-000000000101'::uuid
            when 'CONFIRMED' then '00000000-0000-0000-0000-000000000102'::uuid
            when 'CANCELLED' then '00000000-0000-0000-0000-000000000103'::uuid
            when 'COMPLETED' then '00000000-0000-0000-0000-000000000104'::uuid
            else current_status_id
        end
        where current_status_id is null;
    end if;
end $$;

update orders
set current_status_id = coalesce(
        current_status_id,
        '00000000-0000-0000-0000-000000000101'::uuid
    ),
    status_changed_at = coalesce(status_changed_at, updated_at, created_at, now())
where current_status_id is null
   or status_changed_at is null;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'fk_orders_current_status_id'
    ) then
        alter table orders
            add constraint fk_orders_current_status_id
                foreign key (current_status_id) references order_status_definitions(id);
    end if;
end $$;

alter table orders
    alter column current_status_id set not null;

alter table orders
    alter column status_changed_at set not null;

create table if not exists order_status_history (
    id uuid primary key,
    order_id uuid not null references orders(id) on delete cascade,
    previous_status_id uuid null references order_status_definitions(id),
    current_status_id uuid not null references order_status_definitions(id),
    change_source_type varchar(32) not null,
    changed_by_user_id uuid null,
    comment text null,
    changed_at timestamp with time zone not null
);

create index if not exists idx_order_status_history_order_id_changed_at
    on order_status_history(order_id, changed_at);

create index if not exists idx_order_status_history_current_status_id
    on order_status_history(current_status_id);

insert into order_status_history (
    id,
    order_id,
    previous_status_id,
    current_status_id,
    change_source_type,
    changed_by_user_id,
    comment,
    changed_at
)
select (
           substr(md5(o.id::text || '-initial-order-status-history'), 1, 8) || '-' ||
           substr(md5(o.id::text || '-initial-order-status-history'), 9, 4) || '-' ||
           substr(md5(o.id::text || '-initial-order-status-history'), 13, 4) || '-' ||
           substr(md5(o.id::text || '-initial-order-status-history'), 17, 4) || '-' ||
           substr(md5(o.id::text || '-initial-order-status-history'), 21, 12)
       )::uuid,
       o.id,
       null,
       o.current_status_id,
       'SYSTEM',
       null,
       null,
       coalesce(o.status_changed_at, o.updated_at, o.created_at, now())
from orders o
where not exists (
    select 1
    from order_status_history osh
    where osh.order_id = o.id
);
