package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.LoginRequest;
import com.wms.dto.LoginResponse;
import com.wms.dto.RegisterRequest;
import com.wms.dto.RegisterResponse;
import com.wms.entity.User;
import com.wms.repository.UserRepository;
import com.wms.security.PasswordService;
import com.wms.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final PasswordService passwordService;
    private final TokenService tokenService;

    @Value("${wms.auth.register-default-role:OPERATOR}")
    private String registerDefaultRole;

    @Value("${wms.auth.register-default-status:PENDING}")
    private String registerDefaultStatus;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameAndStatus(request.getUsername(), "ACTIVE")
                .orElseThrow(() -> new BusinessException("\u8d26\u53f7\u4e0d\u5b58\u5728\u6216\u5df2\u88ab\u505c\u7528"));

        if (!passwordService.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("\u7528\u6237\u540d\u6216\u5bc6\u7801\u4e0d\u6b63\u786e");
        }

        if (passwordService.needsUpgrade(user.getPassword())) {
            user.setPassword(passwordService.hash(request.getPassword()));
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return new LoginResponse(
                tokenService.createToken(user),
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRoleCode(),
                permissionService.getMenuPathsByRoleCode(user.getRoleCode())
        );
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(request.getPassword());
        user.setDisplayName(request.getDisplayName().trim());
        user.setPhone(trimToNull(request.getPhone()));
        user.setEmail(trimToNull(request.getEmail()));
        user.setRoleCode(registerDefaultRole);
        user.setStatus(registerDefaultStatus);
        user.setRemark("自助注册申请，待管理员启用");

        User saved = permissionService.saveUser(user);
        String message = "ACTIVE".equals(saved.getStatus())
                ? "注册成功，可以直接登录"
                : "账号申请已提交，请等待管理员启用";
        return new RegisterResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getDisplayName(),
                saved.getRoleCode(),
                saved.getStatus(),
                message
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
