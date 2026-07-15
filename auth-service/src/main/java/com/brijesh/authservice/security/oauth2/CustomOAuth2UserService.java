package com.brijesh.authservice.security.oauth2;

import com.brijesh.authservice.domain.entity.Role;
import com.brijesh.authservice.domain.entity.User;
import com.brijesh.authservice.domain.enums.AuthProvider;
import com.brijesh.authservice.domain.exception.AuthException;
import com.brijesh.authservice.repository.RoleRepository;
import com.brijesh.authservice.repository.UserRepository;
import com.brijesh.authservice.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Set;


@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 1. Let spring fetch user info from Google/GitHub
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. Extract and normalize the provider's user data
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.extract(
                registrationId,oAuth2User
        );

        // 3. Find or create user in our DB
        User user = findOrCreateUser(userInfo);

        // 4. Return our CustomUserDetails (same as local login)
        return new CustomUserDetails(user);
    }

    private User findOrCreateUser(OAuth2UserInfo userInfo) {

        // Check if user already exists by email
        return userRepository.findByEmailWithRoles(userInfo.getEmail())
                .map(existingUser -> updateExistingUser(existingUser,userInfo))
                .orElseGet(() -> createNewUser(userInfo));
    }

    private User updateExistingUser(User user, OAuth2UserInfo userInfo) {

        // If user registered locally before, don't overwrite auth provider
        if (user.getAuthProvider() == AuthProvider.LOCAL){
            throw new AuthException("Email already registered with password. Please login with email and password.");
        }

        // update name in case they changed it on Google/GitHub
        user.setFirstName(userInfo.getFirstName());
        user.setLastName(userInfo.getLastName());
        return userRepository.save(user);
    }


    private User createNewUser(OAuth2UserInfo userInfo){
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new AuthException("Default role not found"));

        User user = User.builder()
                .email(userInfo.getEmail())
                .firstName(userInfo.getFirstName())
                .lastName(userInfo.getLastName())
                .authProvider(userInfo.getProvider())
                .isEnabled(true)                      // OAuth2 users are pre-authorized by Google/GitHub
                .passwordHash(null)                   // no password for OAuth2 users
                .roles(Set.of(userRole))
                .build();

        log.info("New OAuth2 user created: {} via {}", user.getEmail(), userInfo.getProvider());

        return userRepository.save(user);
    }


}
