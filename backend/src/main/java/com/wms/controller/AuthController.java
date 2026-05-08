package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.dto.LoginRequest;
import com.wms.dto.LoginResponse;
import com.wms.dto.RegisterRequest;
import com.wms.dto.RegisterResponse;
import com.wms.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String AUTH_COOKIE_NAME = "WMS_TOKEN";

    private final AuthService authService;

    @Value("${wms.security.token-expiration-minutes:720}")
    private long tokenExpirationMinutes;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResponse loginResponse = authService.login(request);
        writeAuthCookie(response, loginResponse.getToken());
        return ApiResponse.ok(loginResponse);
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    private void writeAuthCookie(HttpServletResponse response, String token) {
        long maxAgeSeconds = tokenExpirationMinutes * 60;
        response.addHeader(HttpHeaders.SET_COOKIE,
                AUTH_COOKIE_NAME + "=" + token +
                        "; Path=/" +
                        "; Max-Age=" + maxAgeSeconds +
                        "; HttpOnly" +
                        "; SameSite=Lax");
    }
}
