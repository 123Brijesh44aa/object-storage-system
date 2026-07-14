package com.brijesh.authservice.security;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class JwtClaimsExtractor {

    private String userUuid;
    private String email;
    private List<String> roles;
    private List<String> permissions;

    public boolean hasRole(String role){
        return roles != null && roles.contains(role);
    }

    public boolean hasPermission(String permission){
        return permissions != null && permissions.contains(permission);
    }
}
