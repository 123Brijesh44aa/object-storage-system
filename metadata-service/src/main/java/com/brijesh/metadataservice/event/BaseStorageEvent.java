package com.brijesh.metadataservice.event;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public abstract class BaseStorageEvent {

    private String eventId;
    private String eventType;
    private String timestamp;
}
