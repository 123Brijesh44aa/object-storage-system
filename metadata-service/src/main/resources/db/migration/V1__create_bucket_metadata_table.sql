create table bucket_metadata
(
    id bigint not null auto_increment,
    name varchar(63) not null,
    owner_uuid varchar(36) not null,
    is_public boolean not null default false,
    region varchar(20) not null default 'us-east-1',
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp,

    primary key (id),
    unique key uk_bucket_name (name),
    index idx_bucket_owner (owner_uuid)
);