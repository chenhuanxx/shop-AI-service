package com.pythonadmin.api;

import com.pythonadmin.common.ApiResponse;
import com.pythonadmin.domain.RoleEntity;
import com.pythonadmin.domain.UserEntity;
import com.pythonadmin.service.AdminService;
import com.pythonadmin.service.CurrentUserService;
import com.pythonadmin.service.RoleService;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RolesController {
    private final CurrentUserService currentUserService;
    private final AdminService adminService;
    private final RoleService roleService;

    public RolesController(CurrentUserService currentUserService, AdminService adminService, RoleService roleService) {
        this.currentUserService = currentUserService;
        this.adminService = adminService;
        this.roleService = roleService;
    }

    public record RoleItem(String id, String name, String description, String createdAt) {}
    public record ListWrapper<T>(List<T> list) {}
    public record PageWrapper<T>(List<T> list, long total) {}
    public record OkWrapper(boolean ok) {}

    public record CreateRoleRequest(@NotBlank String name, String description) {}
    public record DeleteRoleRequest(@NotBlank String id) {}
    public record AssignRoleRequest(int userId, @NotBlank String role) {}

    @GetMapping("/roles")
    public ApiResponse<?> list() {
        UserEntity user = currentUserService.requireUser();
        adminService.requireAdmin(user);
        List<RoleItem> list = roleService.list().stream().map(this::toItem).toList();
        return ApiResponse.ok(new ListWrapper<>(list));
    }

    @GetMapping("/roles/page")
    public ApiResponse<?> listPage(
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "page", required = false, defaultValue = "1") int page,
        @RequestParam(name = "page_size", required = false, defaultValue = "10") int pageSize
    ) {
        UserEntity user = currentUserService.requireUser();
        adminService.requireAdmin(user);
        int safePageSize = Math.max(1, Math.min(pageSize, 200));
        int safePage = Math.max(1, page);

        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<RoleItem> all = roleService.list().stream().map(this::toItem).toList();
        List<RoleItem> filtered = new ArrayList<>();
        for (RoleItem r : all) {
            if (kw.isBlank()) {
                filtered.add(r);
                continue;
            }
            String hay = (safe(r.name()) + " " + safe(r.description())).toLowerCase();
            if (hay.contains(kw)) {
                filtered.add(r);
            }
        }

        long total = filtered.size();
        int fromIndex = (safePage - 1) * safePageSize;
        if (fromIndex >= filtered.size()) {
            return ApiResponse.ok(new PageWrapper<>(List.of(), total));
        }
        int toIndex = Math.min(filtered.size(), fromIndex + safePageSize);
        return ApiResponse.ok(new PageWrapper<>(filtered.subList(fromIndex, toIndex), total));
    }

    @PostMapping("/roles")
    public ApiResponse<?> create(@RequestBody CreateRoleRequest payload) {
        UserEntity user = currentUserService.requireUser();
        adminService.requireAdmin(user);
        RoleEntity r = roleService.create(payload == null ? "" : payload.name(), payload == null ? "" : payload.description());
        return ApiResponse.ok(toItem(r));
    }

    @PostMapping("/roles/delete")
    public ApiResponse<?> delete(@RequestBody DeleteRoleRequest payload) {
        UserEntity user = currentUserService.requireUser();
        adminService.requireAdmin(user);
        roleService.delete(payload == null ? "" : payload.id());
        return ApiResponse.ok(new OkWrapper(true));
    }

    @PostMapping("/roles/assign")
    public ApiResponse<?> assign(@RequestBody AssignRoleRequest payload) {
        UserEntity user = currentUserService.requireUser();
        adminService.requireAdmin(user);
        int userId = payload == null ? 0 : payload.userId();
        String role = payload == null ? "" : payload.role();
        roleService.assignRole(userId, role);
        return ApiResponse.ok(new OkWrapper(true));
    }

    private RoleItem toItem(RoleEntity r) {
        String createdAt = r.getCreatedAt() == null ? "" : r.getCreatedAt().toString();
        return new RoleItem(
            safe(r.getId()),
            safe(r.getName()),
            safe(r.getDescription()),
            createdAt
        );
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
