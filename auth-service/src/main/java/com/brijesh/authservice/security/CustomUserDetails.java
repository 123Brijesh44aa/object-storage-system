package com.brijesh.authservice.security;

import com.brijesh.authservice.domain.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails, OAuth2User {

    @Getter
    private final User user;   // fully entity - all fields accessible

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of(
                "uuid",      user.getUuid(),
                "email",     user.getEmail(),
                "firstName", user.getFirstName(),
                "lastName",  user.getLastName()
        );

    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
       return user.getRoles().stream()
               .flatMap(role -> Stream.concat(
                       Stream.of(new SimpleGrantedAuthority(role.getName())),
                       role.getPermissions().stream()
                               .map(p -> new SimpleGrantedAuthority(p.getName()))
               ))
               .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !user.getIsLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getIsEnabled();
    }

    // --- Convenience methods - avoids calling .getter() everywhere ---------------

    public String getUuid() {
        return user.getUuid();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getFirstName() {
        return user.getFirstName();
    }

    public String getLastName() {
        return user.getLastName();
    }

    public String getFullName(){
        return user.getFirstName()+" "+user.getLastName();
    }

    @Override
    public String getName() {
        return user.getUuid();
    }
}
