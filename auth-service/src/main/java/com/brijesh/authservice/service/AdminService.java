package com.brijesh.authservice.service;


import com.brijesh.authservice.domain.entity.Role;
import com.brijesh.authservice.domain.entity.User;
import com.brijesh.authservice.domain.exception.AuthException;
import com.brijesh.authservice.repository.RoleRepository;
import com.brijesh.authservice.repository.UserRepository;
import com.brijesh.authservice.web.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public List<UserResponse> getAllUsers(){
        return userRepository.findAll()
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUser(String uuid){
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new AuthException("User not found"));
        userRepository.delete(user);
        log.info("User deleted: {}",uuid);
    }

    @Transactional
    public UserResponse assignRole(String uuid,String roleName){
        User user = userRepository.findByUuidWithRoles(uuid)
                .orElseThrow(() -> new AuthException("User not found"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new AuthException("Role not found: "+roleName));

        user.getRoles().add(role);
        userRepository.save(user);

        log.info("Role {} assigned to user {}", roleName, uuid);

        return mapToUserResponse(user);
    }


    @Transactional
    public UserResponse removeRole(String uuid, String roleName){
        User user = userRepository.findByUuidWithRoles(uuid)
                .orElseThrow(() -> new AuthException("User not found"));

        user.getRoles().removeIf(r -> r.getName().equals(roleName));
        userRepository.save(user);

        log.info("Role {} removed from user {}", roleName,uuid);

        return mapToUserResponse(user);
    }

    private UserResponse mapToUserResponse(User user){
        return UserResponse.builder()
                .uuid(user.getUuid())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()) )
                .build();
    }
}
