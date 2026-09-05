package com.brijesh.emailservice.event;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "eventType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = UserRegisteredEvent.class, name = "USER_REGISTERED"),
        @JsonSubTypes.Type(value = EmailVerificationRequestEvent.class, name = "EMAIL_VERIFICATION_REQUESTED"),
        @JsonSubTypes.Type(value = PasswordResetRequestedEvent.class, name = "PASSWORD_RESET_REQUESTED")
})
public class BaseEvent {
    private String eventId;
    private String eventType;
    private Instant timestamp;
}
