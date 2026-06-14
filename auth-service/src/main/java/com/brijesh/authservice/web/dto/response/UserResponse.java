package com.brijesh.authservice.web.dto.response;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Builder
public class UserResponse {
    private String uuid;
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;
}
