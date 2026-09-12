package com.brijesh.metadataservice.consumer;


import com.brijesh.metadataservice.config.KafkaTopics;
import com.brijesh.metadataservice.event.BucketCreatedEvent;
import com.brijesh.metadataservice.event.BucketDeletedEvent;
import com.brijesh.metadataservice.event.ObjectDeletedEvent;
import com.brijesh.metadataservice.event.ObjectUploadedEvent;
import com.brijesh.metadataservice.service.BucketMetadataService;
import com.brijesh.metadataservice.service.ObjectMetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageEventConsumer {

    private final BucketMetadataService bucketService;
    private final ObjectMetadataService objectService;

    @KafkaListener(
            topics = KafkaTopics.BUCKET_CREATED,
            groupId = "metadata-service-group"
    )
    public void handleBucketCreated(
            @Payload BucketCreatedEvent event,
            @Header (KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset ) {
        log.info("Received BUCKET_CREATED | bucket={} owner={}",event.getBucketName(), event.getOwnerUuid());

        try {
            bucketService.createBucket(
                    event.getBucketName(),
                    event.getOwnerUuid(),
                    event.getIsPublic(),
                    event.getRegion()
            );
        } catch (Exception e) {
            log.error("Failed to handle BUCKET_CREATED :{}", e.getMessage());
        }
    }


    @KafkaListener(
            topics = KafkaTopics.BUCKET_DELETED,
            groupId = "metadata-service-group"
    )
    public void handleBucketDeleted(
            @Payload BucketDeletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received BUCKET_DELETED | bucket={}", event.getBucketName());

        try {
            bucketService.deleteBucket(event.getBucketName());
        } catch (Exception e) {
            log.error("Failed to handle BUCKET_DELETED: {}", e.getMessage());
        }
    }


    @KafkaListener(
            topics = KafkaTopics.OBJECT_UPLOADED,
            groupId = "metadata-service-group"
    )
    public void handleObjectUpload(
            @Payload ObjectUploadedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received OBJECT_UPLOADED | bucket={} key={}", event.getBucketName(), event.getObjectKey());

        try {
            objectService.saveObjectMetadata(
                    event.getBucketName(),
                    event.getObjectKey(),
                    event.getContentType(),
                    event.getSizeBytes(),
                    event.getEtag(),
                    event.getStoragePath(),
                    event.getOwnerUuid()
            );
        } catch (Exception e) {
            log.error("Failed to handle OBJECT_UPLOADED : {}", e.getMessage());
        }
    }


    @KafkaListener(
            topics = KafkaTopics.OBJECT_DELETED,
            groupId = "metadata-service-group"
    )
    public void handleObjectDeleted(
            @Payload ObjectDeletedEvent event,
            @Header (KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header (KafkaHeaders.OFFSET) long offset) {
        log.info("Received OBJECT_DELETED | bucket={} key={}", event.getBucketName(), event.getObjectKey());

        try {
            objectService.markObjectDeleted(
                    event.getBucketName(),
                    event.getObjectKey()
            );
        }catch (Exception e){
            log.error("Failed to handle OBJECT_DELETED: {}", e.getMessage());
        }
    }

}













