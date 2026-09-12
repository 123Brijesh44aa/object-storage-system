package com.brijesh.metadataservice.event;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ObjectUploadedEvent extends BaseStorageEvent{

    private String bucketName;
    private String objectKey;
    private String contentType;
    private Long sizeBytes;
    private String etag;
    private String storagePath;
    private String ownerUuid;

}
