package com.brijesh.metadataservice.repository;

import com.brijesh.metadataservice.domain.entity.BucketMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BucketMetadataRepository extends JpaRepository<BucketMetadata,Long> {


    Optional<BucketMetadata> findByName(String name);

    List<BucketMetadata> findAllByOwnerUuid(String ownerUuid);

    boolean existsByName(String name);

    void deleteByName(String name);

}
