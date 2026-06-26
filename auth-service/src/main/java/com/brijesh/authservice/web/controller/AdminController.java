package com.brijesh.authservice.web.controller;


import com.brijesh.authservice.security.SecurityConstants;
import com.brijesh.authservice.service.AdminService;
import com.brijesh.authservice.web.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN) // class level - all methods require ADMIN
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @PreAuthorize(SecurityConstants.HAS_USER_READ) // method level security - also needs user:read
    public ResponseEntity<List<UserResponse>> getAllUsers(){
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @DeleteMapping("/users/{uuid}")
    @PreAuthorize(SecurityConstants.HAS_USER_DELETE)
    public ResponseEntity<Void> deleteUser(@PathVariable String uuid){
        adminService.deleteUser(uuid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{uuid}/roles/{roleName}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_MANAGE)
    public ResponseEntity<UserResponse> assignRole(@PathVariable String uuid, @PathVariable String roleName){
        return ResponseEntity.ok(adminService.assignRole(uuid,roleName));
    }


    @DeleteMapping("/users/{uuid}/roles/{roleName}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_MANAGE)
    public ResponseEntity<UserResponse> removeRole(@PathVariable String uuid, @PathVariable String roleName){
        return ResponseEntity.ok(adminService.removeRole(uuid,roleName));
    }

}
