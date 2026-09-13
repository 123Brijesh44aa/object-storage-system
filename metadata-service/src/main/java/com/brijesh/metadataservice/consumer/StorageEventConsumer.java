package com.brijesh.metadataservice.consumer;


import com.brijesh.metadataservice.config.KafkaTopics;
import com.brijesh.metadataservice.event.BucketCreatedEvent;
import com.brijesh.metadataservice.event.BucketDeletedEvent;
import com.brijesh.metadataservice.event.ObjectDeletedEvent;
import com.brijesh.metadataservice.event.ObjectUploadedEvent;
import com.brijesh.metadataservice.service.BucketMetadataService;
import com.brijesh.metadataservice.service.ObjectMetadataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageEventConsumer {

    private final BucketMetadataService bucketService;
    private final ObjectMetadataService objectService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = KafkaTopics.BUCKET_CREATED,
            groupId = "metadata-service-group"
    )
    public void handleBucketCreated(
            @Payload Map<String, Object> payload,
            @Header (KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset ) {
        log.info("Received BUCKET_CREATED | topic={} offset={}",topic,offset);

        try {
            BucketCreatedEvent event = objectMapper.convertValue(
                    payload, BucketCreatedEvent.class);

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
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received BUCKET_DELETED | topic={} offset={}", topic, offset);

        try {
            BucketDeletedEvent event = objectMapper.convertValue(
                    payload, BucketDeletedEvent.class);

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
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received OBJECT_UPLOADED | topic={} offset={}", topic, offset);

        try {
            ObjectUploadedEvent event = objectMapper.convertValue(
                    payload, ObjectUploadedEvent.class);

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
            @Payload Map<String, Object> payload,
            @Header (KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header (KafkaHeaders.OFFSET) long offset) {
        log.info("Received OBJECT_DELETED | topic={} offset={}", topic,offset);

        try {
            ObjectDeletedEvent event = objectMapper.convertValue(
                    payload, ObjectDeletedEvent.class);

            objectService.markObjectDeleted(
                    event.getBucketName(),
                    event.getObjectKey()
            );
        }catch (Exception e){
            log.error("Failed to handle OBJECT_DELETED: {}", e.getMessage());
        }
    }

}













