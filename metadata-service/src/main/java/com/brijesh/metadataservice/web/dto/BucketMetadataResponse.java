package com.brijesh.metadataservice.web.dto;


import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class BucketMetadataResponse {

    private String name;
    private String ownerUuid;
    private Boolean isPublic;
    private String region;
    private Instant createdAt;
}
