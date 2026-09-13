package com.brijesh.storageservice.service;


import com.brijesh.storageservice.client.MetadataServiceClient;
import com.brijesh.storageservice.client.dto.ObjectMetadataResponse;
import com.brijesh.storageservice.infrastructure.kafka.StorageEventPublisher;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectService {

    private final MinioClient minioClient;
    private final StorageEventPublisher eventPublisher;
    private final MetadataServiceClient metadataServiceClient;

    // Upload
    public String uploadObject(String bucketName, String objectKey, MultipartFile file, String ownerUuid) {
        try {
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

            // Storage path = bucketName/objectKey
            String storagePath = bucketName + "/" + objectKey;

            // upload to MinIO
            ObjectWriteResponse response = minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );

            String etag = response.etag();

            log.info("Object uploaded to MinIO: {}/{}", bucketName, objectKey);

            // Publish event -> Metadata service saves metadata asynchronously
            eventPublisher.publishObjectUploaded(
                    bucketName,
                    objectKey,
                    contentType,
                    file.getSize(),
                    etag,
                    storagePath,
                    ownerUuid
            );

            return etag;
        } catch (Exception e) {
            log.error("Failed to upload {}/{}: {}", bucketName, objectKey, e.getMessage());
            throw new RuntimeException("Failed to upload object: "+ e.getMessage());
        }
    }


    // Download
    public DownloadResult downloadObject(String bucketName, String objectKey) {
        try {
            // Step 1 : Ask metadata service for object info
            ObjectMetadataResponse metadata = metadataServiceClient
                    .getObjectMetadata(bucketName, objectKey);

            if (metadata == null) {
                throw new RuntimeException(
                        "Metadata service unavailable, Cannot download file."
                );
            }

            // step 2: fetch from minio using storage path
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );

            log.info("Object downloaded from MinIO: {}/{}", bucketName, objectKey);

            return new DownloadResult(
                    stream,
                    metadata.getContentType(),
                    metadata.getSizeBytes(),
                    objectKey
            );

        }catch (RuntimeException e){
            throw e;
        } catch (Exception e){
            log.error("Failed to download {}/{}: {}", bucketName, objectKey, e.getMessage());
            throw new RuntimeException("Failed to download object: "+ e.getMessage());
        }
    }

    // Delete
    public void deleteObject(String bucketName, String objectKey, String ownerUuid) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );

            log.info("Object deleted from MinIO: {}/{}", bucketName, objectKey);

            eventPublisher.publishObjectDeleted(bucketName, objectKey, ownerUuid);
        } catch (Exception e) {
            log.error("Failed to delete {}/{}: {}", bucketName, objectKey, e.getMessage());
            throw new RuntimeException("Failed to delete object: "+e.getMessage());
        }
    }



    public record DownloadResult(
            InputStream stream,
            String contentType,
            Long sizeBytes,
            String fileName
    ) {
    }


}
