package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.RoleMenuRequest;
import com.wms.entity.BaseEntity;
import com.wms.entity.Menu;
import com.wms.entity.Role;
import com.wms.entity.RoleMenu;
import com.wms.entity.User;
import com.wms.entity.UserRole;
import com.wms.repository.MenuRepository;
import com.wms.repository.RoleMenuRepository;
import com.wms.repository.RoleRepository;
import com.wms.repository.UserRepository;
import com.wms.repository.UserRoleRepository;
import com.wms.security.PasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final PasswordService passwordService;

    public Map<String, Object> overview() {
        List<Map<String, Object>> users = userRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(this::toUserView)
                .toList();
        List<Role> roles = roleRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<Menu> menus = menuRepository.findAll(Sort.by(Sort.Direction.ASC, "sortNo"));
        Map<Long, List<Long>> roleMenus = roleMenuRepository.findAll().stream()
                .collect(Collectors.groupingBy(RoleMenu::getRoleId,
                        Collectors.mapping(RoleMenu::getMenuId, Collectors.toList())));

        Map<String, Object> data = new HashMap<>();
        data.put("users", users);
        data.put("roles", roles);
        data.put("menus", menus);
        data.put("roleMenus", roleMenus);
        return data;
    }

    public Map<String, Object> toUserView(User user) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", user.getId());
        item.put("username", user.getUsername());
        item.put("displayName", user.getDisplayName());
        item.put("phone", user.getPhone());
        item.put("email", user.getEmail());
        item.put("roleCode", user.getRoleCode());
        item.put("status", user.getStatus());
        item.put("remark", user.getRemark());
        item.put("createdAt", user.getCreatedAt());
        item.put("updatedAt", user.getUpdatedAt());
        return item;
    }

    @Transactional
    public User saveUser(User user) {
        requireText(user.getUsername(), "\u7528\u6237\u540d\u4e0d\u80fd\u4e3a\u7a7a");
        requireText(user.getDisplayName(), "\u663e\u793a\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        requireText(user.getRoleCode(), "\u89d2\u8272\u7f16\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        if (user.getStatus() == null || user.getStatus().isBlank()) {
            user.setStatus("ACTIVE");
        }
        if (user.getId() != null) {
            userRepository.findById(user.getId()).ifPresent(existing -> {
                preserveAuditFields(user, existing);
                if (user.getPassword() == null || user.getPassword().isBlank()) {
                    user.setPassword(existing.getPassword());
                }
            });
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new BusinessException("\u5bc6\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (!passwordService.isHashed(user.getPassword())) {
            user.setPassword(passwordService.hash(user.getPassword()));
        }
        User saved = userRepository.save(user);
        syncUserRole(saved);
        return saved;
    }

    @Transactional
    public Role saveRole(Role role) {
        requireText(role.getRoleCode(), "\u89d2\u8272\u7f16\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        requireText(role.getRoleName(), "\u89d2\u8272\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        if (role.getStatus() == null || role.getStatus().isBlank()) {
            role.setStatus("ACTIVE");
        }
        if (role.getId() != null) {
            roleRepository.findById(role.getId()).ifPresent(existing -> preserveAuditFields(role, existing));
        }
        return roleRepository.save(role);
    }

    @Transactional
    public Menu saveMenu(Menu menu) {
        requireText(menu.getMenuCode(), "\u83dc\u5355\u7f16\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        requireText(menu.getMenuName(), "\u83dc\u5355\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        if (menu.getMenuType() == null || menu.getMenuType().isBlank()) {
            menu.setMenuType("MENU");
        }
        if (menu.getSortNo() == null) {
            menu.setSortNo(0);
        }
        if (menu.getVisible() == null) {
            menu.setVisible(Boolean.TRUE);
        }
        if (menu.getStatus() == null || menu.getStatus().isBlank()) {
            menu.setStatus("ACTIVE");
        }
        if (menu.getId() != null) {
            menuRepository.findById(menu.getId()).ifPresent(existing -> preserveAuditFields(menu, existing));
        }
        return menuRepository.save(menu);
    }

    @Transactional
    public void saveRoleMenus(Long roleId, RoleMenuRequest request) {
        roleRepository.findById(roleId).orElseThrow(() -> new BusinessException("Role not found"));
        roleMenuRepository.deleteByRoleId(roleId);
        List<Long> menuIds = request.getMenuIds() == null ? List.of() : request.getMenuIds();
        menuIds.forEach(menuId -> {
            RoleMenu roleMenu = new RoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            roleMenuRepository.save(roleMenu);
        });
    }

    public List<String> getMenuPathsByRoleCode(String roleCode) {
        Role role = roleRepository.findByRoleCode(roleCode)
                .orElseThrow(() -> new BusinessException("Role not found"));
        List<Menu> allVisibleMenus = menuRepository.findByStatusAndVisibleOrderBySortNoAsc("ACTIVE", Boolean.TRUE);
        List<RoleMenu> mappings = roleMenuRepository.findByRoleId(role.getId());

        if ("ADMIN".equals(roleCode) || mappings.isEmpty()) {
            return allVisibleMenus.stream()
                    .map(Menu::getMenuPath)
                    .filter(path -> path != null && !path.isBlank())
                    .toList();
        }

        Set<Long> menuIdSet = mappings.stream().map(RoleMenu::getMenuId).collect(Collectors.toSet());
        return allVisibleMenus.stream()
                .filter(menu -> menuIdSet.contains(menu.getId()))
                .map(Menu::getMenuPath)
                .filter(path -> path != null && !path.isBlank())
                .toList();
    }

    private void syncUserRole(User user) {
        if (user.getRoleCode() == null || user.getRoleCode().isBlank()) {
            return;
        }
        Role role = roleRepository.findByRoleCode(user.getRoleCode())
                .orElseThrow(() -> new BusinessException("Role code not found"));
        userRoleRepository.deleteByUserId(user.getId());
        userRoleRepository.flush();
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRoleRepository.save(userRole);
    }

    private void preserveAuditFields(BaseEntity target, BaseEntity existing) {
        target.setCreatedAt(existing.getCreatedAt());
        target.setUpdatedAt(existing.getUpdatedAt());
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
    }
}
