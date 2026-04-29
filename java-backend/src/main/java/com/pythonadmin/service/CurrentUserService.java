package com.pythonadmin.service;

import com.pythonadmin.domain.UserEntity;
import com.pythonadmin.domain.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class CurrentUserService {
    private final UserRepository userRepo;

    public CurrentUserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public UserEntity requireUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(UNAUTHORIZED);
        }
        String username = String.valueOf(auth.getPrincipal()).trim();
        if (username.isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED);
        }
        return userRepo.findByUsername(username).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
    }
}
