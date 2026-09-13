package com.brijesh.storageservice.infrastructure.kafka;


import com.brijesh.storageservice.config.KafkaTopics;
import com.brijesh.storageservice.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageEventPublisher {

    private final KafkaTemplate<String,Object> kafkaTemplate;

    public void publishBucketCreated(String bucketName, String ownerUuid, Boolean isPublic, String region) {
        BucketCreatedEvent event = BucketCreatedEvent.builder()
                .eventType("BUCKET_CREATED")
                .bucketName(bucketName)
                .ownerUuid(ownerUuid)
                .isPublic(isPublic)
                .region(region)
                .build();
        event.initDefaults();
        publish(KafkaTopics.BUCKET_CREATED, bucketName, event);
    }

    public void publishBucketDeleted(String bucketName, String ownerUuid) {
        BucketDeletedEvent event = BucketDeletedEvent.builder()
                .eventType("BUCKET_DELETED")
                .bucketName(bucketName)
                .ownerUuid(ownerUuid)
                .build();
        event.initDefaults();
        publish(KafkaTopics.BUCKET_DELETED, bucketName, event);
    }

    public void publishObjectUploaded(String bucketName, String objectKey,
                                      String contentType, Long sizeBytes,
                                      String etag, String storagePath,
                                      String ownerUuid) {
        ObjectUploadEvent event = ObjectUploadEvent.builder()
                .eventType("OBJECT_UPLOADED")
                .bucketName(bucketName)
                .objectKey(objectKey)
                .contentType(contentType)
                .sizeBytes(sizeBytes)
                .etag(etag)
                .storagePath(storagePath)
                .ownerUuid(ownerUuid)
                .build();
        event.initDefaults();
        publish(KafkaTopics.OBJECT_UPLOADED, bucketName + "/" + objectKey, event);
    }

    public void publishObjectDeleted(String bucketName, String objectKey,
                                     String ownerUuid) {
        ObjectDeletedEvent event = ObjectDeletedEvent.builder()
                .eventType("OBJECT_DELETED")
                .bucketName(bucketName)
                .objectKey(objectKey)
                .ownerUuid(ownerUuid)
                .build();
        event.initDefaults();
        publish(KafkaTopics.OBJECT_DELETED, bucketName + "/" + objectKey, event);
    }

    private void publish(String topic, String key, BaseStorageEvent event) {
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} to {}: {}",
                                event.getEventType(), topic, ex.getMessage());
                    } else {
                        log.info("Published {} to topic={} partition={} offset={}",
                                event.getEventType(),
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
