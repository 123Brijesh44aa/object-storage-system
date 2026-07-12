package com.brijesh.authservice.domain.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends BaseEntity{

    @Column(nullable = false, unique = true, length = 100)
    private String name;   // e.g. "user:read", "user:delete"

    @Column(length = 255)
    private String description;
}
