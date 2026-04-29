package com.pythonadmin.service;

import com.pythonadmin.domain.UserEntity;
import com.pythonadmin.domain.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class UserService {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void resetPassword(int userId, String passwordRaw) {
        String password = passwordRaw == null ? "" : passwordRaw.trim();
        if (password.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "密码不能为空");
        }
        if (password.length() < 6) {
            throw new ResponseStatusException(BAD_REQUEST, "密码至少6位");
        }
        UserEntity u = userRepo.findById(userId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "用户不存在"));
        u.setHashedPassword(passwordEncoder.encode(password));
        userRepo.save(u);
    }
}
