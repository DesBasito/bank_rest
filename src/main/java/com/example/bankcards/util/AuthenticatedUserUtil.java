package com.example.bankcards.util;

import com.example.bankcards.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserUtil {
    private final JwtUtil util;

    public Long getUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return util.extractUserId(token);
        }
        throw new AccessDeniedException("JWT token not found");
    }

    public String getUserNameFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return util.extractFullName(token);
        }
        throw new AccessDeniedException("JWT token not found");
    }
}
