alter table if exists admin_user
    add column if not exists role varchar(64);

do $$
begin
    if to_regclass('admin_user') is not null then
        update admin_user
           set role = 'SUPERADMIN'
         where role is null;
    end if;
end $$;

alter table if exists admin_user
    alter column role set not null;

alter table if exists admin_user
    add column if not exists is_active boolean not null default true;

alter table if exists admin_user
    add column if not exists deleted_at timestamptz;

create index if not exists idx_admin_user_role_active
    on admin_user(role, is_active)
    where deleted_at is null;

create index if not exists idx_admin_user_deleted_at
    on admin_user(deleted_at);
