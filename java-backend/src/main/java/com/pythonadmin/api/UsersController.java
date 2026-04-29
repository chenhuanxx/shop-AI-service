package com.pythonadmin.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pythonadmin.domain.UserEntity;
import com.pythonadmin.domain.UserRepository;
import com.pythonadmin.domain.RoleRepository;
import com.pythonadmin.service.AdminService;
import com.pythonadmin.service.CurrentUserService;
import com.pythonadmin.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
public class UsersController {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepo;
    private final CurrentUserService currentUserService;
    private final AdminService adminService;
    private final UserService userService;

    public UsersController(
        UserRepository userRepo,
        PasswordEncoder passwordEncoder,
        RoleRepository roleRepo,
        CurrentUserService currentUserService,
        AdminService adminService,
        UserService userService
    ) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.roleRepo = roleRepo;
        this.currentUserService = currentUserService;
        this.adminService = adminService;
        this.userService = userService;
    }

    public record UserItem(
        int id,
        String username,
        String role,
        String phone,
        String address,
        String gender,
        @JsonProperty("birth_date") String birthDate,
        @JsonProperty("is_active") boolean isActive,
        @JsonProperty("created_at") String createdAt
    ) {}

    public record UserPage(
        int page,
        @JsonProperty("page_size") int pageSize,
        long total,
        List<UserItem> items
    ) {}

    public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank String password,
        String phone,
        String address,
        String gender,
        @JsonProperty("birth_date") String birthDate,
        @JsonProperty("is_active") Boolean isActive,
        String role
    ) {}

    public record UpdateUserRequest(
        @NotBlank String username,
        String password,
        String phone,
        String address,
        String gender,
        @JsonProperty("birth_date") String birthDate,
        @JsonProperty("is_active") Boolean isActive,
        String role
    ) {}

    public record ResetPasswordRequest(@NotBlank String password) {}

    @GetMapping("/users")
    public UserPage list(
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
        @RequestParam(name = "keyword", required = false) String keyword
    ) {
        UserEntity me = currentUserService.requireUser();
        adminService.requireAdmin(me);
        int safePage = Math.max(1, page);
        int safeSize = Math.min(200, Math.max(1, pageSize));
        String kw = keyword == null ? "" : keyword.trim();
        PageRequest pr = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "id"));

        Page<UserEntity> p = kw.isBlank()
            ? userRepo.findAll(pr)
            : userRepo.findByUsernameContainingIgnoreCase(kw, pr);

        List<UserItem> items = p.getContent().stream().map(UsersController::toItem).toList();
        return new UserPage(safePage, safeSize, p.getTotalElements(), items);
    }

    @PostMapping("/users")
    public void create(@RequestBody CreateUserRequest payload) {
        UserEntity me = currentUserService.requireUser();
        adminService.requireAdmin(me);
        String username = payload == null || payload.username() == null ? "" : payload.username().trim();
        String password = payload == null || payload.password() == null ? "" : payload.password().trim();
        boolean isActive = payload != null && payload.isActive() != null ? payload.isActive() : true;
        String roleRaw = payload == null || payload.role() == null ? "" : payload.role().trim();
        String phone = payload == null || payload.phone() == null ? "" : payload.phone().trim();
        String address = payload == null || payload.address() == null ? "" : payload.address().trim();
        String gender = payload == null || payload.gender() == null ? "" : payload.gender().trim();
        String birthDate = payload == null || payload.birthDate() == null ? "" : payload.birthDate().trim();

        if (username.isBlank() || password.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "用户名或密码不能为空");
        }
        if (userRepo.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "用户名已存在");
        }

        UserEntity u = new UserEntity();
        u.setUsername(username);
        u.setHashedPassword(passwordEncoder.encode(password));
        u.setActive(isActive);
        u.setPhone(phone);
        u.setAddress(address);
        u.setGender(gender);
        u.setBirthDate(birthDate);
        if (!roleRaw.isBlank()) {
            roleRepo.findByName(roleRaw).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "角色不存在"));
            u.setRole(roleRaw);
        } else {
            u.setRole("user");
        }
        userRepo.save(u);
    }

    @PutMapping("/users/{id}")
    public void update(@PathVariable("id") int id, @RequestBody UpdateUserRequest payload) {
        UserEntity me = currentUserService.requireUser();
        adminService.requireAdmin(me);
        UserEntity u = userRepo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "用户不存在"));

        String username = payload == null || payload.username() == null ? "" : payload.username().trim();
        String password = payload == null || payload.password() == null ? "" : payload.password().trim();
        boolean isActive = payload != null && payload.isActive() != null ? payload.isActive() : u.isActive();
        String roleRaw = payload == null || payload.role() == null ? "" : payload.role().trim();
        String phone = payload == null || payload.phone() == null ? "" : payload.phone().trim();
        String address = payload == null || payload.address() == null ? "" : payload.address().trim();
        String gender = payload == null || payload.gender() == null ? "" : payload.gender().trim();
        String birthDate = payload == null || payload.birthDate() == null ? "" : payload.birthDate().trim();

        if (username.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "用户名不能为空");
        }
        userRepo.findByUsername(username).ifPresent(existing -> {
            if (existing.getId() != null && existing.getId() != id) {
                throw new ResponseStatusException(BAD_REQUEST, "用户名已存在");
            }
        });

        u.setUsername(username);
        u.setActive(isActive);
        u.setPhone(phone);
        u.setAddress(address);
        u.setGender(gender);
        u.setBirthDate(birthDate);
        if (!roleRaw.isBlank()) {
            roleRepo.findByName(roleRaw).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "角色不存在"));
            u.setRole(roleRaw);
        }
        if (!password.isBlank()) {
            u.setHashedPassword(passwordEncoder.encode(password));
        }
        userRepo.save(u);
    }

    @PostMapping({"/users/{id}/reset-password", "/api/users/{id}/reset-password"})
    public void resetPassword(@PathVariable("id") int id, @RequestBody ResetPasswordRequest payload) {
        UserEntity me = currentUserService.requireUser();
        adminService.requireAdmin(me);
        String password = payload == null || payload.password() == null ? "" : payload.password().trim();
        userService.resetPassword(id, password);
    }

    @GetMapping("/users/export")
    public void export(
        @RequestParam(name = "keyword", required = false) String keyword,
        HttpServletResponse response
    ) throws IOException {
        UserEntity me = currentUserService.requireUser();
        adminService.requireAdmin(me);
        String kw = keyword == null ? "" : keyword.trim();
        List<UserEntity> users = kw.isBlank()
            ? userRepo.findAll(Sort.by(Sort.Direction.DESC, "id"))
            : userRepo.findByUsernameContainingIgnoreCase(kw, Sort.by(Sort.Direction.DESC, "id"));

        StringBuilder sb = new StringBuilder();
        sb.append("id,username,role,phone,address,gender,birth_date,is_active,created_at\n");
        for (UserEntity u : users) {
            Integer id = u.getId();
            String username = u.getUsername();
            String role = u.getRole();
            String phone = u.getPhone();
            String address = u.getAddress();
            String gender = u.getGender();
            String birthDate = u.getBirthDate();
            boolean isActive = u.isActive();
            Instant createdAt = u.getCreatedAt();
            sb.append(csv(String.valueOf(id == null ? "" : id)));
            sb.append(',');
            sb.append(csv(username == null ? "" : username));
            sb.append(',');
            sb.append(csv(role == null ? "" : role));
            sb.append(',');
            sb.append(csv(phone == null ? "" : phone));
            sb.append(',');
            sb.append(csv(address == null ? "" : address));
            sb.append(',');
            sb.append(csv(gender == null ? "" : gender));
            sb.append(',');
            sb.append(csv(birthDate == null ? "" : birthDate));
            sb.append(',');
            sb.append(csv(isActive ? "true" : "false"));
            sb.append(',');
            sb.append(csv(createdAt == null ? "" : createdAt.toString()));
            sb.append('\n');
        }

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        response.setStatus(200);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE + ";charset=utf-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.csv");
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }

    private static UserItem toItem(UserEntity u) {
        String createdAt = u.getCreatedAt() == null ? "" : u.getCreatedAt().toString();
        return new UserItem(
            u.getId() == null ? 0 : u.getId(),
            u.getUsername() == null ? "" : u.getUsername(),
            u.getRole() == null ? "" : u.getRole(),
            u.getPhone() == null ? "" : u.getPhone(),
            u.getAddress() == null ? "" : u.getAddress(),
            u.getGender() == null ? "" : u.getGender(),
            u.getBirthDate() == null ? "" : u.getBirthDate(),
            u.isActive(),
            createdAt
        );
    }

    private static String csv(String raw) {
        String v = raw == null ? "" : raw;
        String escaped = v.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
