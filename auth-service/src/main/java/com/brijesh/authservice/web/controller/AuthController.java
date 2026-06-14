package com.brijesh.authservice.web.controller;

import com.brijesh.authservice.service.AuthService;
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

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(request,httpRequest));
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
