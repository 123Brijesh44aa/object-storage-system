package com.brijesh.authservice.security.oauth2;


import com.brijesh.authservice.domain.enums.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OAuth2UserInfo {

    private String providerId;  // Google's "sub" or Github's "id"
    private String email;
    private String firstName;
    private String lastName;
    private AuthProvider provider;
}
