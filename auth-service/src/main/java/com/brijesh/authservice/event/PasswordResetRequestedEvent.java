package com.brijesh.authservice.event;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class PasswordResetRequestedEvent extends BaseEvent{

    private String userUuid;
    private String email;
    private String firstName;
    private String resetToken;   // raw token
}
