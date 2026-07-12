package com.brijesh.authservice.domain.entity;


import com.brijesh.authservice.domain.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_uuid", columnList = "uuid")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity{

    @Column(nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 255)            // nullable -> OAuth2 users have no password
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING) // Store "LOCAL" not "0" in DB
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isEnabled = false;   // false until email verified

    @Column(nullable = false)
    @Builder.Default
    private Boolean isLocked = false;   // true after brute force detected


    @Column(nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();



    @PrePersist
    private void prePersist(){
        if (this.uuid == null){
            this.uuid = UUID.randomUUID().toString();
        }
    }
}
