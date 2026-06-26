package com.brijesh.authservice.web.controller;

import com.brijesh.authservice.security.CustomUserDetails;
import com.brijesh.authservice.web.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                UserResponse.builder()
                        .uuid(userDetails.getUuid())
                        .email(userDetails.getEmail())
                        .firstName(userDetails.getFirstName())
                        .lastName(userDetails.getLastName())
                        .roles(userDetails.getAuthorities().stream()
                                .map(a -> a.getAuthority())
                                .filter(a -> a.startsWith("ROLE_"))
                                .collect(Collectors.toSet())
                        )
                        .build()
        );
    }
}