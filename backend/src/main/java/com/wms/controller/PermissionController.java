package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.dto.RoleMenuRequest;
import com.wms.entity.Menu;
import com.wms.entity.Role;
import com.wms.entity.User;
import com.wms.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping("/overview")
    public ApiResponse<?> overview() {
        return ApiResponse.ok(permissionService.overview());
    }

    @PostMapping("/users")
    public ApiResponse<?> createUser(@RequestBody User user) {
        return ApiResponse.ok(permissionService.saveUser(user));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        return ApiResponse.ok(permissionService.saveUser(user));
    }

    @PostMapping("/roles")
    public ApiResponse<?> createRole(@RequestBody Role role) {
        return ApiResponse.ok(permissionService.saveRole(role));
    }

    @PutMapping("/roles/{id}")
    public ApiResponse<?> updateRole(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        return ApiResponse.ok(permissionService.saveRole(role));
    }

    @PostMapping("/menus")
    public ApiResponse<?> createMenu(@RequestBody Menu menu) {
        return ApiResponse.ok(permissionService.saveMenu(menu));
    }

    @PutMapping("/menus/{id}")
    public ApiResponse<?> updateMenu(@PathVariable Long id, @RequestBody Menu menu) {
        menu.setId(id);
        return ApiResponse.ok(permissionService.saveMenu(menu));
    }

    @PostMapping("/roles/{roleId}/menus")
    public ApiResponse<?> saveRoleMenus(@PathVariable Long roleId, @RequestBody RoleMenuRequest request) {
        permissionService.saveRoleMenus(roleId, request);
        return ApiResponse.ok("saved", null);
    }
}
