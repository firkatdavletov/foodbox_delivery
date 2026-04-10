create table if not exists legal_documents (
    type varchar(64) primary key,
    title varchar(255) not null,
    subtitle varchar(512),
    content text not null default '',
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);
