package com.brijesh.authservice.service;


import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuditService {

    // Auth Events
    public void logLogin(String userUuid, String email, String ip){
        log.info("AUDIT",
                StructuredArguments.keyValue("action", "USER_LOGIN"),
                StructuredArguments.keyValue("userUuid", userUuid),
                StructuredArguments.keyValue("email", email),
                StructuredArguments.keyValue("ip", ip),
                StructuredArguments.keyValue("status", "SUCCESS")
                );
    }

    public void logFailedLogin(String email, String ip, String reason){
        log.warn("AUDIT",
                StructuredArguments.keyValue("action", "USER_LOGIN"),
                StructuredArguments.keyValue("email", email),
                StructuredArguments.keyValue("ip", ip),
                StructuredArguments.keyValue("status", "FAILURE"),
                StructuredArguments.keyValue("reason",reason)
        );
    }

    public void logRegistration(String userUuid, String email, String ip){
        log.info("AUDIT",
                StructuredArguments.keyValue("action", "USER_REGISTER"),
                StructuredArguments.keyValue("userUuid", userUuid),
                StructuredArguments.keyValue("email", email),
                StructuredArguments.keyValue("ip", ip),
                StructuredArguments.keyValue("status", "SUCCESS")
        );
    }

    public void logLogout(String userUuid, String ip){
        log.info("AUDIT",
                StructuredArguments.keyValue("action", "USER_LOGOUT"),
                StructuredArguments.keyValue("userUuid", userUuid),
                StructuredArguments.keyValue("ip", ip),
                StructuredArguments.keyValue("status", "SUCCESS")
        );
    }

    public void logPasswordChange(String userUuid, String ip){
        log.info("AUDIT",
                StructuredArguments.keyValue("action", "PASSWORD_CHANGE"),
                StructuredArguments.keyValue("userUuid", userUuid),
                StructuredArguments.keyValue("ip", ip),
                StructuredArguments.keyValue("status", "SUCCESS")
        );
    }

    public void logPasswordReset(String email, String ip){
        log.info("AUDIT",
                StructuredArguments.keyValue("action", "PASSWORD_RESET"),
                StructuredArguments.keyValue("email", email),
                StructuredArguments.keyValue("ip", ip),
                StructuredArguments.keyValue("status", "SUCCESS")
        );
    }

    public void logTokenRefresh(String userUuid, String ip){
        log.info("AUDIT",
                StructuredArguments.keyValue("action", "TOKEN_REFRESH"),
                StructuredArguments.keyValue("userUuid", userUuid),
                StructuredArguments.keyValue("ip", ip),
                StructuredArguments.keyValue("status", "SUCCESS")
        );
    }

    public void logSuspiciousActivity(String userUuid,String email,String reason,String ip){
        log.warn("AUDIT",
                StructuredArguments.keyValue("action", "SUSPICIOUS_ACTIVITY"),
                StructuredArguments.keyValue("userUuid", userUuid),
                StructuredArguments.keyValue("email", email),
                StructuredArguments.keyValue("reason", reason),
                StructuredArguments.keyValue("ip", ip),
                StructuredArguments.keyValue("status", "ALERT")
                );
    }
}
