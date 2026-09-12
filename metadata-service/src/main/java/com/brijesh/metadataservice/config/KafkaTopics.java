package com.brijesh.metadataservice.config;

public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String BUCKET_CREATED = "bucket.created";
    public static final String BUCKET_DELETED = "bucket.deleted";
    public static final String OBJECT_UPLOADED = "object.uploaded";
    public static final String OBJECT_DELETED = "object.deleted";
}
