package com.brijesh.authservice.event;


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
public abstract class BaseEvent {

    private String eventId = UUID.randomUUID().toString();
    private String eventType;
    private Instant timestamp = Instant.now();
}
