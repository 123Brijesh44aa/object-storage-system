package com.brijesh.storageservice.event;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class BucketCreatedEvent extends BaseStorageEvent{

    private String bucketName;
    private String ownerUuid;
    private Boolean isPublic;
    private String region;
}
