package com.brijesh.emailservice.config;

public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String USER_REGISTERED = "user.registered";
    public static final String EMAIL_VERIFICATION_REQUESTED = "user.email.verification.requested";
    public static final String PASSWORD_RESET_REQUESTED = "user.password.reset.requested";
}
