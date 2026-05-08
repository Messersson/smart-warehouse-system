package com.wms.security;

import java.util.List;

public record AuthenticatedUser(
        Long userId,
        String username,
        String displayName,
        String roleCode,
        List<String> menuPaths
) {
}
