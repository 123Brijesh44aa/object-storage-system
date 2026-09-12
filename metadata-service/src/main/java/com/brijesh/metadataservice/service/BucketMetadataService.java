package com.brijesh.metadataservice.service;


import com.brijesh.metadataservice.domain.entity.BucketMetadata;
import com.brijesh.metadataservice.repository.BucketMetadataRepository;
import com.brijesh.metadataservice.web.dto.BucketMetadataResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BucketMetadataService {

    private final BucketMetadataRepository bucketRepository;

    @Transactional
    public void createBucket(String bucketName, String ownerUuid,
                             Boolean isPublic, String region) {
        if (bucketRepository.existsByName(bucketName)) {
            log.warn("Bucket already exists: {}", bucketName);
            return;
        }

        BucketMetadata bucket = BucketMetadata.builder()
                .name(bucketName)
                .ownerUuid(ownerUuid)
                .isPublic(isPublic != null ? isPublic : false)
                .region(region != null ? region : "us-east-1")
                .build();

        bucketRepository.save(bucket);
        log.info("Bucket metadata created: {}", bucketName);
    }

    @Transactional
    public void deleteBucket(String bucketName) {
        bucketRepository.findByName(bucketName)
                .ifPresent(bucket -> {
                    bucketRepository.delete(bucket);
                    log.info("Bucket metadata deleted: {}", bucketName);
                });
    }

    public List<BucketMetadataResponse> getBucketsForUser(String ownerUuid) {
        return bucketRepository.findAllByOwnerUuid(ownerUuid)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public BucketMetadataResponse getBucket(String bucketName) {
        return bucketRepository.findByName(bucketName)
                .map(this::toResponse)
                .orElseThrow(() ->
                        new RuntimeException("Bucket not found: " + bucketName));
    }

    private BucketMetadataResponse toResponse(BucketMetadata bucket) {
        return BucketMetadataResponse.builder()
                .name(bucket.getName())
                .ownerUuid(bucket.getOwnerUuid())
                .isPublic(bucket.getIsPublic())
                .region(bucket.getRegion())
                .createdAt(bucket.getCreatedAt())
                .build();
    }
}
