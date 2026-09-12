package com.brijesh.metadataservice.service;


import com.brijesh.metadataservice.domain.entity.ObjectMetadata;
import com.brijesh.metadataservice.repository.ObjectMetadataRepository;
import com.brijesh.metadataservice.web.dto.ObjectMetadataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectMetadataService {

    private final ObjectMetadataRepository objectRepository;

    @Transactional
    public void saveObjectMetadata(String bucketName, String objectKey,
                                   String contentType, Long sizeBytes,
                                   String etag, String storagePath,
                                   String ownerUuid) {
        ObjectMetadata existing = objectRepository
                .findByBucketNameAndObjectKeyAndIsDeletedFalse(bucketName, objectKey).orElse(null);

        if (existing != null){
            // update existing (overwrite)
            existing.setContentType(contentType);
            existing.setSizeBytes(sizeBytes);
            existing.setEtag(etag);
            existing.setStoragePath(storagePath);
            objectRepository.save(existing);
            log.info("Object metadata updated: {}/{}", bucketName,objectKey);
        } else {
            ObjectMetadata object = ObjectMetadata.builder()
                    .bucketName(bucketName)
                    .objectKey(objectKey)
                    .contentType(contentType)
                    .sizeBytes(sizeBytes)
                    .etag(etag)
                    .storagePath(storagePath)
                    .ownerUuid(ownerUuid)
                    .build();
            objectRepository.save(object);
            log.info("Object metadata saved: {}/{}", bucketName,objectKey);
        }
    }

    @Transactional
    public void markObjectDeleted(String bucketName, String objectKey) {
        objectRepository
                .findByBucketNameAndObjectKeyAndIsDeletedFalse(bucketName,objectKey)
                .ifPresent(obj -> {
                    obj.setIsDeleted(true);
                    objectRepository.save(obj);
                    log.info("Object metadata marked deleted: {}/{}", bucketName,objectKey);
                });
    }

    public ObjectMetadataResponse getObjectMetadata(String bucketName, String objectKey) {
        return objectRepository
                .findByBucketNameAndObjectKeyAndIsDeletedFalse(bucketName,objectKey)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Object not found: "+ bucketName +"/" + objectKey));
    }

    public List<ObjectMetadataResponse> listObjects(String bucketName,String prefix) {
        List<ObjectMetadata> objects;

        if (prefix != null && !prefix.isEmpty()) {
            objects = objectRepository
                    .findAllByBucketNameAndObjectKeyStartingWithAndIsDeletedFalse(bucketName,prefix);
        } else {
            objects = objectRepository
                    .findAllByBucketNameAndIsDeletedFalse(bucketName);
        }

        return objects.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }



    private ObjectMetadataResponse toResponse(ObjectMetadata obj){
        return ObjectMetadataResponse.builder()
                .bucketName(obj.getBucketName())
                .objectKey(obj.getObjectKey())
                .contentType(obj.getContentType())
                .sizeBytes(obj.getSizeBytes())
                .etag(obj.getEtag())
                .storagePath(obj.getStoragePath())
                .ownerUuid(obj.getOwnerUuid())
                .createdAt(obj.getCreatedAt())
                .build();
    }
}
