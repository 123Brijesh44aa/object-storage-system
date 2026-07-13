package com.brijesh.authservice.web.controller;

import com.brijesh.authservice.service.AuthService;
import com.brijesh.authservice.service.EmailVerificationService;
import com.brijesh.authservice.web.dto.request.LoginRequest;
import com.brijesh.authservice.web.dto.request.RefreshTokenRequest;
import com.brijesh.authservice.web.dto.request.RegisterRequest;
import com.brijesh.authservice.web.dto.response.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    public ResponseEntity<Map<String,String>> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest){
        authService.register(request,httpRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("message","Registration successful. Please check you email to verify your account."));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String,String>> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);

        return ResponseEntity.ok(
                Map.of("message","Email verified successfully. You can now login.")
        );
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String,String>> resendVerification(@RequestBody Map<String,String> body){
        String email = body.get("email");
        authService.resendVerification(email);

        // Always return success - don't reveal if email exists
        return ResponseEntity.ok(
                Map.of("message","If this email is registered, a verification link has been sent.")
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request,httpRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request,HttpServletRequest httpRequest){
        return ResponseEntity.ok(authService.refresh(request,httpRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader, @Valid @RequestBody RefreshTokenRequest request){
        String accessToken = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        authService.logout(accessToken,request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}
