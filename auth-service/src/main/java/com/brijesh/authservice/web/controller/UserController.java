package com.brijesh.authservice.web.controller;

import com.brijesh.authservice.security.CustomUserDetails;
import com.brijesh.authservice.service.PasswordResetService;
import com.brijesh.authservice.web.dto.request.ChangePasswordRequest;
import com.brijesh.authservice.web.dto.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final PasswordResetService passwordResetService;

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

    @PostMapping("/me/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String,String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader("Authorization") String authHeader){

        String accessToken = authHeader.startsWith("Bearer ")
                ? authHeader.substring(7)
                : authHeader;

        passwordResetService.changePassword(
                userDetails.getUser(),
                request.getCurrentPassword(),
                request.getNewPassword(),
                accessToken
        );

        return ResponseEntity.ok(Map.of("message", "Password changed successfully. Please login again."));
    }
}