package com.brijesh.authservice.security;

public final class SecurityConstants {

    private SecurityConstants(){}

    // Roles
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";


    // Permissions
    public static final String PERM_USER_READ = "user:read";
    public static final String PERM_USER_UPDATE = "user:update";
    public static final String PERM_USER_DELETE = "user:delete";
    public static final String PERM_ROLE_READ = "role:read";
    public static final String PERM_ROLE_MANAGE = "role:manage";
    public static final String PERM_PERMISSION_MANAGE = "permission:manage";

    // SpEL(spring expression language) expressions for @PreAuthorize
    public static final String HAS_ROLE_ADMIN = "hasRole('ADMIN')";
    public static final String HAS_USER_READ = "hasAuthority('user:read')";
    public static final String HAS_USER_UPDATE = "hasAuthority('user:update')";
    public static final String HAS_USER_DELETE = "hasAuthority('user:delete')";
    public static final String HAS_ROLE_MANAGE = "hasAuthority('role:manage')";

}
