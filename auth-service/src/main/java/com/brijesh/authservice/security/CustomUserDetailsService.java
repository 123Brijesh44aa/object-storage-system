package com.brijesh.authservice.security;

import com.brijesh.authservice.domain.entity.User;
import com.brijesh.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
       var user = userRepository
               .findByEmailWithRoles(email)
               .orElseThrow(() ->
                       new UsernameNotFoundException("User not found : "+ email));
       return new CustomUserDetails(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserByUuid(String uuid){
        var user = userRepository.findByUuidWithRoles(uuid)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found: "+uuid));
        return new CustomUserDetails(user);
    }
}
