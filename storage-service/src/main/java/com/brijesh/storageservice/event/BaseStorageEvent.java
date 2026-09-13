package com.brijesh.storageservice.event;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class BaseStorageEvent {

    private String eventId = UUID.randomUUID().toString();
    private String eventType;
    private Instant timestamp = Instant.now();

    public void initDefaults() {
        if (this.eventId == null) {
            this.eventId = UUID.randomUUID().toString();
        }
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
    }
}
