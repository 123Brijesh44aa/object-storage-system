package com.brijesh.metadataservice.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ObjectDeletedEvent extends BaseStorageEvent{

    private String bucketName;
    private String objectKey;
    private String ownerUuid;
}
