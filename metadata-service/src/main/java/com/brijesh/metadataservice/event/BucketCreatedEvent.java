package com.brijesh.metadataservice.event;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BucketCreatedEvent extends BaseStorageEvent{

    private String bucketName;
    private String ownerUuid;
    private Boolean isPublic;
    private String region;
}
