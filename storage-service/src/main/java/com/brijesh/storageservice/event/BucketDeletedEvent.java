package com.brijesh.storageservice.event;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class BucketDeletedEvent extends BaseStorageEvent{

    private String bucketName;
    private String ownerUuid;

}
