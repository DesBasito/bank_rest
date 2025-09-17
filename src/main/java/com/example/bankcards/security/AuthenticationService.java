package com.example.bankcards.security;

import com.example.bankcards.dto.mappers.UserMapper;
import com.example.bankcards.dto.users.SignInRequest;
import com.example.bankcards.dto.users.SignUpRequest;
import com.example.bankcards.entity.RefreshSession;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repositories.RefreshSessionRepository;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserService userService;
    private final UserMapper userMapper;
    private final JwtUtil jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RefreshSessionRepository refreshSessionRepository;
    private final DeviceFingerprintService deviceFingerprintService;

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_TOKEN_PATH = "/api/auth";

    public void signUp(SignUpRequest request) {
        User user = userMapper.toEntity(request);
        userService.create(user);
    }

    public String signIn(SignInRequest signInRequest, HttpServletResponse response, HttpServletRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                signInRequest.getPhoneNumber(),
                signInRequest.getPassword()
        ));

        UserDetails user = userService
                .userDetailsService()
                .loadUserByUsername(signInRequest.getPhoneNumber());

        String access = jwtService.generateToken(user);
        List<RefreshSession> sessions = refreshSessionRepository.findByUserOrderByCreatedAtAsc((User) user, PageRequest.of(0, 5));
        if (sessions.size() >= 5) {
            refreshSessionRepository.deleteAll(sessions);
        }

        String fingerprint = getOrGenerateFingerprint(request);
        generateRefreshToken((User) user, fingerprint, request, response);

        return access;
    }

    @Transactional
    public String refreshToken(UUID oldToken, HttpServletResponse response, HttpServletRequest request) {
        try {
            if (oldToken == null) {
                throw new IllegalArgumentException("Old token is null");
            }
            RefreshSession session = refreshSessionByRefreshToken(oldToken);
            User user = session.getUser();

            String fingerprint = getOrGenerateFingerprint(request);
            validateRefreshToken(session, fingerprint);
            generateRefreshToken(user, fingerprint, request, response);
            refreshSessionRepository.deleteByRefreshToken(oldToken);
            return jwtService.generateToken(user);
        } catch (NoSuchElementException | IllegalArgumentException e) {
            clearRefreshTokenCookie(response);
            throw e;
        }
    }

    private String getOrGenerateFingerprint(HttpServletRequest request) {
        String fingerprint = request.getHeader("Fingerprint");

        if (!StringUtils.hasText(fingerprint)) {
            fingerprint = deviceFingerprintService.generateFingerprint(request);
            log.debug("Generated automatic fingerprint: {}", fingerprint);
        } else {
            log.debug("Using provided fingerprint: {}", fingerprint);
        }

        return fingerprint;
    }

    private void setRefreshToken(UUID refreshToken, HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken.toString());
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath(REFRESH_TOKEN_PATH);
        cookie.setMaxAge(60 * 60 * 24 * 30);
        response.addCookie(cookie);
    }

    private void generateRefreshToken(User user, String fingerprint, HttpServletRequest request, HttpServletResponse response) {
        UUID newToken = UUID.randomUUID();
        RefreshSession refreshSession = RefreshSession.builder()
                .refreshToken(newToken)
                .user(user)
                .fingerprint(fingerprint)
                .ua(request.getHeader("User-Agent"))
                .ip(getClientIpAddress(request))
                .expiresIn(System.currentTimeMillis() / 1000 + (60 * 60 * 24 * 30))
                .build();
        refreshSessionRepository.save(refreshSession);
        setRefreshToken(newToken, response);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIP)) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }

    private RefreshSession refreshSessionByRefreshToken(UUID refreshToken) {
        return refreshSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new NoSuchElementException("Refresh token not found"));
    }

    private void validateRefreshToken(RefreshSession session, String fingerprint) {
        if (!session.getFingerprint().equals(fingerprint)) {
            log.warn("Fingerprint mismatch for refresh token. Expected: {}, Got: {}",
                    session.getFingerprint(), fingerprint);
            throw new IllegalArgumentException("Fingerprint mismatch");
        }
        if (session.getExpiresIn() < System.currentTimeMillis() / 1000) {
            throw new IllegalArgumentException("Refresh token expired");
        }
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath(REFRESH_TOKEN_PATH);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}