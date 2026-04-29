package com.pythonadmin.service;

import com.pythonadmin.domain.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class AdminService {
    public boolean isAdmin(UserEntity user) {
        if (user == null) {
            return false;
        }
        String role = user.getRole() == null ? "" : user.getRole().trim();
        if ("admin".equalsIgnoreCase(role)) {
            return true;
        }
        String username = user.getUsername() == null ? "" : user.getUsername().trim();
        return "admin".equalsIgnoreCase(username);
    }

    public void requireAdmin(UserEntity user) {
        if (!isAdmin(user)) {
            throw new ResponseStatusException(FORBIDDEN, "无权限");
        }
    }
}
