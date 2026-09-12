create table object_metadata
(
    id bigint not null auto_increment,
    bucket_name varchar(63) not null,
    object_key varchar(1024) not null,
    content_type varchar(255) not null,
    size_bytes bigint not null,
    etag varchar(255) null,
    storage_path varchar(1024) not null,
    owner_uuid varchar(36) not null,
    is_deleted boolean not null default false,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp on update current_timestamp,

    primary key (id),
    unique key uk_bucket_key (bucket_name, object_key(255)),
    index idx_object_owner (owner_uuid),
    index idx_object_bucket (bucket_name),
    index idx_storage_path (storage_path(255))
);