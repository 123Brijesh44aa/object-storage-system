package com.brijesh.authservice.service;


import com.brijesh.authservice.domain.entity.User;
import com.brijesh.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Service
public class LoginAttemptService {

    private final UserRepository userRepository;
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    // checking if the account is currently locked.
    // if lock duration has passed, auto-unlock.
    @Transactional
    public boolean isAccountLocked(User user){
        if (user.getLockedUntil() == null){
            return false;
        }

        if (user.getLockedUntil().isBefore(Instant.now())){
            // Lock expired - auto unlock
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
            log.info("Account auto unlocked : {}",user.getEmail());
            return false;
        }

        return true;
    }

    @Transactional
    public void recordFailedAttempts(User user){
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_ATTEMPTS){
            user.setLockedUntil(Instant.now().plusSeconds(LOCK_DURATION_MINUTES * 60));
            log.warn("Account locked due to {} failed attempts: {}",attempts,user.getEmail());
        }

        userRepository.save(user);
    }

    @Transactional
    public void recordSuccessfulLogin(User user){
        if (user.getFailedLoginAttempts() > 0){
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
    }
}
