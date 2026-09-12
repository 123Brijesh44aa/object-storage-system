package com.brijesh.metadataservice.web.dto;


import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ObjectMetadataResponse {

    private String bucketName;
    private String objectKey;
    private String contentType;
    private Long sizeBytes;
    private String etag;
    private String storagePath;
    private String ownerUuid;
    private Instant createdAt;
}
