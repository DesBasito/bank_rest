package com.example.bankcards.security;

import com.example.bankcards.entity.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
//import kg.manurov.muslim_motors.domain.dto.users.AuthResponse;
//import kg.manurov.muslim_motors.domain.dto.users.SignInRequest;
//import kg.manurov.muslim_motors.domain.dto.users.SignUpRequest;
//import kg.manurov.muslim_motors.domain.models.RefreshSession;
//import kg.manurov.muslim_motors.domain.models.User;
//import kg.manurov.muslim_motors.repositories.repo.RefreshSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserService userService;
    private final JwtUtil jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RefreshSessionRepository refreshSessionRepository;
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_TOKEN_PATH = "/api/auth";

    /**
     * Регистрация пользователя
     *
     * @param request данные пользователя
     */
    public void signUp(SignUpRequest request) {
        User user = User.builder()
                .name(request.getName())
                .surname(request.getSurname())
                .middleName(request.getMiddleName())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPasswordHash()))
                .role(request.getRole())
                .enabled(true)
                .build();

        userService.create(user);
    }


    public AuthResponse signIn(SignInRequest signInRequest, HttpServletResponse response, HttpServletRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                signInRequest.getPhoneNumber(),
                signInRequest.getPassword()
        ));

        UserDetails user = userService
                .userDetailsService()
                .loadUserByUsername(signInRequest.getPhoneNumber());

        String access =  jwtService.generateToken(user);
        List<RefreshSession> sessions = refreshSessionRepository.findByUserOrderByCreatedAtAsc((User) user, PageRequest.of(0, 5));
        if (sessions.size() >= 5) {
            refreshSessionRepository.deleteAll(sessions);
        }
        generateRefreshToken((User) user, request.getHeader("Fingerprint"), request, response);
        return new AuthResponse(access);
    }

    @Transactional
    public AuthResponse refreshToken(UUID oldToken, HttpServletResponse response, HttpServletRequest request) {
        try {
            if (oldToken == null){
                throw new IllegalArgumentException("Old token is null");
            }
            RefreshSession session = refreshSessionByRefreshToken(oldToken);
            User user = session.getUser();
            validateRefreshToken(session, request.getHeader("Fingerprint"));
            generateRefreshToken(user, request.getHeader("Fingerprint"), request, response);
            refreshSessionRepository.deleteByRefreshToken(oldToken);
            String accessToken = jwtService.generateToken(user);
            return new AuthResponse(accessToken);
        } catch (NoSuchElementException | IllegalArgumentException e) {
            clearRefreshTokenCookie(response);
            throw e;
        }

    }

    private void setRefreshToken(UUID refreshToken, HttpServletResponse response){
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
                .userAgent(request.getHeader("User-Agent"))
                .ip(request.getRemoteAddr())
                .createdAt(Timestamp.from(Instant.now()))
                .expiresIn(System.currentTimeMillis() / 1000 + (60 * 60 * 24 * 30))
                .build();
        refreshSessionRepository.save(refreshSession);
        setRefreshToken(newToken, response);
    }

    private RefreshSession refreshSessionByRefreshToken(UUID refreshToken) {
        return refreshSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new NoSuchElementException("Refresh token not found"));
    }

    private void validateRefreshToken(RefreshSession session, String fingerprint) {
        if(!session.getFingerprint().equals(fingerprint)){
            throw new IllegalArgumentException("Fingerprint mismatch");
        }
        if (session.getExpiresIn() < System.currentTimeMillis() / 1000) {
            throw new IllegalArgumentException("Refresh expired");
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
