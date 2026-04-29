package com.pythonadmin.service;

import com.pythonadmin.domain.RoleEntity;
import com.pythonadmin.domain.RoleRepository;
import com.pythonadmin.domain.UserEntity;
import com.pythonadmin.domain.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class RoleService {
    private final RoleRepository roleRepo;
    private final UserRepository userRepo;

    public RoleService(RoleRepository roleRepo, UserRepository userRepo) {
        this.roleRepo = roleRepo;
        this.userRepo = userRepo;
    }

    public List<RoleEntity> list() {
        return roleRepo.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Transactional
    public RoleEntity create(String nameRaw, String descriptionRaw) {
        String name = nameRaw == null ? "" : nameRaw.trim();
        if (name.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "角色名不能为空");
        }
        roleRepo.findByName(name).ifPresent(x -> {
            throw new ResponseStatusException(BAD_REQUEST, "角色已存在");
        });
        RoleEntity r = new RoleEntity();
        r.setId("r_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        r.setName(name);
        r.setDescription(descriptionRaw == null ? "" : descriptionRaw.trim());
        return roleRepo.save(r);
    }

    @Transactional
    public void delete(String idRaw) {
        String id = idRaw == null ? "" : idRaw.trim();
        if (id.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "缺少角色 id");
        }
        RoleEntity role = roleRepo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "角色不存在"));
        if ("admin".equalsIgnoreCase(role.getName()) || "user".equalsIgnoreCase(role.getName())) {
            throw new ResponseStatusException(BAD_REQUEST, "内置角色不可删除");
        }
        roleRepo.delete(role);
    }

    @Transactional
    public void assignRole(int userId, String roleNameRaw) {
        String roleName = roleNameRaw == null ? "" : roleNameRaw.trim();
        if (roleName.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "缺少角色名");
        }
        roleRepo.findByName(roleName).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "角色不存在"));
        UserEntity u = userRepo.findById(userId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "用户不存在"));
        u.setRole(roleName);
        userRepo.save(u);
    }
}

