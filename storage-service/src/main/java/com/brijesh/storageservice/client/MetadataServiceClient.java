package com.brijesh.storageservice.client;


import com.brijesh.storageservice.client.dto.ObjectMetadataResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "metadata-service",
        fallback = MetadataServiceClientFallback.class
)
public interface MetadataServiceClient {

    @GetMapping("/api/v1/metadata/buckets/{bucketName}/objects/{objectKey}")
    ObjectMetadataResponse getObjectMetadata(@PathVariable String bucketName, @PathVariable String objectKey);
}
