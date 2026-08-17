package com.brijesh.emailservice.event;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PasswordResetRequestedEvent {

    private String userUuid;
    private String email;
    private String firstName;
    private String resetToken;

}
