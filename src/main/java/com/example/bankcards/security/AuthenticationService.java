package com.example.bankcards.security;

import com.example.bankcards.dto.users.AuthResponse;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserService userService;
    private final JwtUtil jwtService;
    private final DeviceFingerprintService deviceFingerprintService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RefreshSessionRepository refreshSessionRepository;

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_TOKEN_PATH = "/api/auth";

    /**
     * Регистрация пользователя
     */
    public void signUp(SignUpRequest request) {
        User user = User.builder()
                .firstName(request.getName())
                .lastName(request.getSurname())
                .middleName(request.getMiddleName())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPasswordHash()))
                .enabled(true)
                .roles(request.getRoleIds()
                        .stream()
                        .map(e -> {
                            Role r = new Role();
                            r.setId(e.getId());
                            return r;
                        }).collect(Collectors.toSet()))
                .build();

        userService.create(user);
    }

    /**
     * Авторизация пользователя с автоматической генерацией fingerprint
     */
    public AuthResponse signIn(SignInRequest signInRequest, HttpServletResponse response, HttpServletRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                signInRequest.getPhoneNumber(),
                signInRequest.getPassword()
        ));

        UserDetails user = userService
                .userDetailsService()
                .loadUserByUsername(signInRequest.getPhoneNumber());

        String access = jwtService.generateToken(user);

        // Очищаем старые сессии если их слишком много
        List<RefreshSession> sessions = refreshSessionRepository.findByUserOrderByCreatedAtAsc((User) user, PageRequest.of(0, 5));
        if (sessions.size() >= 5) {
            refreshSessionRepository.deleteAll(sessions);
        }

        // Автоматически генерируем fingerprint на основе HTTP заголовков
        String deviceFingerprint = deviceFingerprintService.generateFingerprint(request);

        generateRefreshToken((User) user, deviceFingerprint, request, response);
        return new AuthResponse(access);
    }

    @Transactional
    public AuthResponse refreshToken(UUID oldToken, HttpServletResponse response, HttpServletRequest request) {
        try {
            if (oldToken == null) {
                throw new IllegalArgumentException("Old token is null");
            }
            RefreshSession session = refreshSessionByRefreshToken(oldToken);
            User user = session.getUser();

            // Проверяем fingerprint (для обновления токена используем тот же fingerprint)
            String deviceFingerprint = deviceFingerprintService.generateFingerprint(request);
            validateRefreshToken(session, deviceFingerprint);

            generateRefreshToken(user, deviceFingerprint, request, response);
            refreshSessionRepository.deleteByRefreshToken(oldToken);
            String accessToken = jwtService.generateToken(user);
            return new AuthResponse(accessToken);

        } catch (NoSuchElementException | IllegalArgumentException e) {
            clearRefreshTokenCookie(response);
            throw e;
        }
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
                .createdAt(Instant.now())
                .expiresIn(System.currentTimeMillis() / 1000 + (60 * 60 * 24 * 30))
                .build();
        refreshSessionRepository.save(refreshSession);
        setRefreshToken(newToken, response);
    }

    private RefreshSession refreshSessionByRefreshToken(UUID refreshToken) {
        return refreshSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new NoSuchElementException("Refresh token not found"));
    }

    private void validateRefreshToken(RefreshSession session, String currentFingerprint) {
        // Для refresh token можем быть менее строгими к fingerprint
        // так как устройство может изменить некоторые параметры
        if (session.getExpiresIn() < System.currentTimeMillis() / 1000) {
            throw new IllegalArgumentException("Refresh token expired");
        }
        // Можно добавить дополнительные проверки IP или User-Agent при необходимости
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath(REFRESH_TOKEN_PATH);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }
}