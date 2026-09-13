package com.brijesh.storageservice.web.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ObjectUploadResponse {
    private String bucketName;
    private String objectKey;
    private String etag;
    private Long sizeBytes;
    private String contentType;
    private String message;
}