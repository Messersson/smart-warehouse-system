package com.wms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.common.ApiResponse;
import com.wms.entity.User;
import com.wms.repository.UserRepository;
import com.wms.service.PermissionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ApiSecurityInterceptor implements HandlerInterceptor {

    private static final String AUTH_COOKIE_NAME = "WMS_TOKEN";

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/actuator/health",
            "/actuator/info"
    );

    private static final Map<String, List<String>> API_MENU_PATHS = new LinkedHashMap<>();

    static {
        API_MENU_PATHS.put("/api/dashboard", List.of("/dashboard"));
        API_MENU_PATHS.put("/api/warehouses", List.of("/master/warehouses"));
        API_MENU_PATHS.put("/api/locations", List.of("/master/locations"));
        API_MENU_PATHS.put("/api/owners", List.of("/master/owners"));
        API_MENU_PATHS.put("/api/suppliers", List.of("/master/suppliers"));
        API_MENU_PATHS.put("/api/customers", List.of("/master/customers"));
        API_MENU_PATHS.put("/api/products", List.of("/master/products"));
        API_MENU_PATHS.put("/api/inbounds", List.of("/inbounds"));
        API_MENU_PATHS.put("/api/outbounds", List.of("/outbounds"));
        API_MENU_PATHS.put("/api/stocks", List.of("/stocks"));
        API_MENU_PATHS.put("/api/alerts", List.of("/alerts"));
        API_MENU_PATHS.put("/api/notifications", List.of("/alerts"));
        API_MENU_PATHS.put("/api/permissions", List.of("/permissions"));
        API_MENU_PATHS.put("/api/stock-takes", List.of("/stock-takes"));
        API_MENU_PATHS.put("/api/billing", List.of("/billing"));
        API_MENU_PATHS.put("/api/approvals", List.of("/approvals"));
        API_MENU_PATHS.put("/api/exceptions", List.of("/exceptions"));
        API_MENU_PATHS.put("/api/scan", List.of("/pda", "/inbounds", "/outbounds", "/stocks"));
    }

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublicPath(request.getRequestURI())) {
            return true;
        }
        if (!request.getRequestURI().startsWith("/api/")) {
            return true;
        }

        Optional<TokenClaims> claims = tokenService.parseToken(resolveToken(request));
        if (claims.isEmpty()) {
            writeError(response, HttpStatus.UNAUTHORIZED, "\u767b\u5f55\u5df2\u8fc7\u671f\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55");
            return false;
        }

        Optional<User> userResult = userRepository.findById(claims.get().userId())
                .filter(user -> "ACTIVE".equals(user.getStatus()));
        if (userResult.isEmpty()) {
            writeError(response, HttpStatus.UNAUTHORIZED, "\u8d26\u53f7\u4e0d\u5b58\u5728\u6216\u5df2\u88ab\u505c\u7528");
            return false;
        }

        User user = userResult.get();
        List<String> menuPaths = permissionService.getMenuPathsByRoleCode(user.getRoleCode());
        AuthContext.setCurrentUser(new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRoleCode(),
                menuPaths
        ));

        if (!hasAccess(request.getRequestURI(), user.getRoleCode(), menuPaths)) {
            AuthContext.clear();
            writeError(response, HttpStatus.FORBIDDEN, "\u65e0\u6743\u8bbf\u95ee\u8be5\u8d44\u6e90");
            return false;
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        AuthContext.clear();
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean hasAccess(String apiPath, String roleCode, List<String> menuPaths) {
        if ("ADMIN".equals(roleCode)) {
            return true;
        }

        List<String> requiredMenuPaths = API_MENU_PATHS.entrySet().stream()
                .filter(entry -> apiPath.startsWith(entry.getKey()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(List.of());
        if (requiredMenuPaths.isEmpty()) {
            return true;
        }
        return requiredMenuPaths.stream().anyMatch(menuPaths::contains);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        String accessToken = request.getHeader("X-Access-Token");
        if (accessToken != null && !accessToken.isBlank()) {
            return accessToken;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (AUTH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws Exception {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(message));
    }
}
