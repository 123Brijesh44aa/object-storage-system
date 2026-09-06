package com.brijesh.authservice.web.controller;

import com.brijesh.authservice.domain.entity.User;
import com.brijesh.authservice.domain.exception.AuthException;
import com.brijesh.authservice.repository.UserRepository;
import com.brijesh.authservice.security.CustomUserDetails;
import com.brijesh.authservice.service.PasswordResetService;
import com.brijesh.authservice.web.dto.request.ChangePasswordRequest;
import com.brijesh.authservice.web.dto.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final PasswordResetService passwordResetService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal Object principal, HttpServletRequest request) {
        // Coming through Gateway - principal is uuid String
        if (principal instanceof String uuid) {
            var user = userRepository.findByUuidWithRoles(uuid)
                    .orElseThrow(() -> new AuthException("User not found"));
            return ResponseEntity.ok(mapToUserResponse(user));
        }

        // Direct access - principal is CustomUserDetails
        if (principal instanceof CustomUserDetails userDetails){
            return ResponseEntity.ok(mapToUserResponse(userDetails.getUser()));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    private UserResponse mapToUserResponse(User user){
        return UserResponse.builder()
                .uuid(user.getUuid())
                .email(user.getEmail())
                .firstName(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles().stream()
                        .map(r -> r.getName())
                        .collect(Collectors.toSet())
                )
                .build();
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