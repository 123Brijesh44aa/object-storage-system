package com.brijesh.authservice.security;

public class ServiceAuthHeaders {

    private ServiceAuthHeaders(){}

    public static final String USER_UUID = "X-User-Uuid";
    public static final String USER_EMAIL = "X-User-Email";
    public static final String USER_ROLES = "X-User-Roles";
    public static final String USER_PERMISSIONS = "X-User-Permissions";
    public static final String REQUEST_ID = "X-Request_Id";  // for tracing
}
