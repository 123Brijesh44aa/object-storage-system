package com.brijesh.storageservice.client;


import com.brijesh.storageservice.client.dto.ObjectMetadataResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MetadataServiceClientFallback implements MetadataServiceClient{
    @Override
    public ObjectMetadataResponse getObjectMetadata(String bucketName, String objectKey) {
        log.error("Metadata service unavailable. "+ "Cannot get metadata for {}/{}", bucketName,objectKey);
        return null;  // caller checks for null and return 503
    }
}
