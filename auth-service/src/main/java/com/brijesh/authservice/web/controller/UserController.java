package com.brijesh.authservice.web.controller;

import com.brijesh.authservice.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/me")
    public Object getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return java.util.Map.of(
                "uuid", userDetails.getUuid(),
                "email", userDetails.getEmail(),
                "fullName", userDetails.getFullName(),
                "authorities", userDetails.getAuthorities()
        );
    }
}