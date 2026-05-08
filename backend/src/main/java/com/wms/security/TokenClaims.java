package com.wms.security;

public record TokenClaims(
        Long userId,
        String username,
        String roleCode,
        long expiresAt
) {
}
