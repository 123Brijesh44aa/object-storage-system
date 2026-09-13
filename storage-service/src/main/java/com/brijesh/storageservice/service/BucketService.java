package com.brijesh.storageservice.service;


import com.brijesh.storageservice.infrastructure.kafka.StorageEventPublisher;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BucketService {

    private final MinioClient minioClient;
    private final StorageEventPublisher eventPublisher;

    public void createBucket(String bucketName, String ownerUuid, Boolean isPublic, String region) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (exists) {
                throw new RuntimeException("Bucket already exists: "+bucketName);
            }

            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            log.info("Bucket created in MinIO: {}", bucketName);

            // Publish event -> Metadata service will save metadata

            eventPublisher.publishBucketCreated(bucketName,ownerUuid, isPublic, region);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create bucket {}: {}", bucketName, e.getMessage());
            throw new RuntimeException("Failed to create bucket: "+ e.getMessage());
        }
    }


    public void deleteBucket(String bucketName, String ownerUuid) {
        try {
            // Remove all objects first
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : objects) {
                Item item = result.get();
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(item.objectName())
                                .build()
                );
            }

            // Delete bucket
            minioClient.removeBucket(
                    RemoveBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            log.info("Bucket deleted from MinIO: {}", bucketName);

            eventPublisher.publishBucketDeleted(bucketName,ownerUuid);
        } catch (Exception e) {
            log.error("Failed to delete bucket {}: {}", bucketName, e.getMessage());
            throw new RuntimeException("Failed to delete bucket: "+e.getMessage());
        }
    }


}
