package com.brijesh.storageservice.web.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BucketResponse {
    private String bucketName;
    private String ownerUuid;
    private Boolean isPublic;
    private String region;
    private String message;
}