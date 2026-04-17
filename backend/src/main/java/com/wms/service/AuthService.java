package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.LoginRequest;
import com.wms.dto.LoginResponse;
import com.wms.entity.User;
import com.wms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PermissionService permissionService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameAndStatus(request.getUsername(), "ACTIVE")
                .orElseThrow(() -> new BusinessException("Account does not exist or has been disabled"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new BusinessException("Username or password is incorrect");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return new LoginResponse(
                "demo-token-" + user.getId() + "-" + System.currentTimeMillis(),
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRoleCode(),
                permissionService.getMenuPathsByRoleCode(user.getRoleCode())
        );
    }
}
