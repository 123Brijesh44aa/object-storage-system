package com.brijesh.metadataservice.event;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BucketDeletedEvent extends BaseStorageEvent{

    private String bucketName;
    private String ownerUuid;
}
