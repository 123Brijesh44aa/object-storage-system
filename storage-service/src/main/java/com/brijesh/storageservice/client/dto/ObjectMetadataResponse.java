package com.brijesh.storageservice.client.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
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
