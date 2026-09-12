package com.brijesh.metadataservice.repository;

import com.brijesh.metadataservice.domain.entity.ObjectMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ObjectMetadataRepository extends JpaRepository<ObjectMetadata, Long> {


    Optional<ObjectMetadata> findByBucketNameAndObjectKeyAndIsDeletedFalse(
            String bucketName, String objectKey);

    List<ObjectMetadata> findAllByBucketNameAndIsDeletedFalse(
            String bucketName);

    List<ObjectMetadata> findAllByBucketNameAndObjectKeyStartingWithAndIsDeletedFalse(
            String bucketName, String prefix);

    Optional<ObjectMetadata> findByStoragePath(String storagePath);

}
