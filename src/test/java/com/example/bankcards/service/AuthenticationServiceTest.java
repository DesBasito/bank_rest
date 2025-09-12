package com.example.bankcards.service;

import com.example.bankcards.dto.users.AuthResponse;
import com.example.bankcards.dto.users.SignInRequest;
import com.example.bankcards.dto.users.SignUpRequest;
import com.example.bankcards.entity.RefreshSession;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.RefreshSessionRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.AuthenticationService;
import com.example.bankcards.util.JwtUtil;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты сервиса аутентификации")
class AuthenticationServiceTest {

    private final UserRepository repository = Mockito.mock(UserRepository.class);

    @Spy
    private UserService userService = new UserService(repository);

    @Mock
    private JwtUtil jwtService;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshSessionRepository refreshSessionRepository;

    @Mock
    private HttpServletRequest request = new MockHttpServletRequest();

    @Mock
    private HttpServletResponse response = new MockHttpServletResponse();

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private SignUpRequest signUpRequest;
    private SignInRequest signInRequest;
    private RefreshSession testSession;
    private UUID testRefreshToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("ID1988767")
                .firstName("Тест")
                .lastName("Тестов")
                .middleName("Тестович")
                .phoneNumber("500123321")
                .password("hashed123")
                .roles(Set.of(new Role(2L,"ADMIN","KUKU",null)))
                .enabled(true)
                .build();

        signUpRequest = new SignUpRequest();
        signUpRequest.setName("Тест");
        signUpRequest.setSurname("Тестов");
        signUpRequest.setMiddleName("Тестович");
        signUpRequest.setPhoneNumber("500123321");
        signUpRequest.setPasswordHash("qwe");
        signUpRequest.setRoleIds(Set.of(new Role(2L,"ADMIN","ADMINOV",null)));

        signInRequest = new SignInRequest();
        signInRequest.setPhoneNumber("500123321");
        signInRequest.setPassword("qwe");

        testRefreshToken = UUID.randomUUID();
        testSession = createValidRefreshSession(testUser, testRefreshToken, "test-fingerprint-123");
    }

    private RefreshSession createValidRefreshSession(User user, UUID token, String fingerprint) {
        RefreshSession session = new RefreshSession();
        session.setId(1);
        session.setUser(user);
        session.setRefreshToken(token);
        session.setFingerprint(fingerprint);
        session.setUa("Mozilla/5.0");
        session.setIp("192.168.1.1");
        session.setExpiresIn(System.currentTimeMillis() / 1000 + 86400);
        return session;
    }

    private RefreshSession createRandomRefreshSession(User user) {
        return createValidRefreshSession(
                user,
                UUID.randomUUID(),
                "fingerprint-" + System.currentTimeMillis()
        );
    }

    private RefreshSession createExpiredRefreshSession(User user, UUID token, String fingerprint) {
        RefreshSession session = createValidRefreshSession(user, token, fingerprint);
        session.setExpiresIn(System.currentTimeMillis() / 1000 - 3600);
        return session;
    }

    @Test
    @DisplayName("Должен зарегистрировать пользователя")
    void shouldSignUpUser() {
        authenticationService.signUp(signUpRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getFirstName()).isEqualTo("Тест");
        assertThat(capturedUser.getPassword()).startsWith("$2a$10$");
        assertThat(capturedUser.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Должен выполнить вход пользователя")
    void shouldSignInUser() {
        when(userService.userDetailsService()).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername("+996500123321")).thenReturn(testUser);
        when(jwtService.generateToken(testUser)).thenReturn("accessToken123");
        when(refreshSessionRepository.findByUserOrderByCreatedAtAsc(eq(testUser), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());
        when(request.getHeader("Fingerprint")).thenReturn("test-fingerprint-123");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        AuthResponse result = authenticationService.signIn(signInRequest, response, request);

        assertThat(result.getAccessToken()).isEqualTo("accessToken123");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshSessionRepository).save(any(RefreshSession.class));
        verify(response).addCookie(any(Cookie.class));
    }

    @Test
    @DisplayName("Должен удалить старые сессии при превышении лимита")
    void shouldDeleteOldSessionsWhenLimitExceeded() {
        List<RefreshSession> oldSessions = Arrays.asList(
                createRandomRefreshSession(testUser),
                createRandomRefreshSession(testUser),
                createRandomRefreshSession(testUser),
                createRandomRefreshSession(testUser),
                createRandomRefreshSession(testUser)
        );

        when(userService.userDetailsService()).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername("+996500123321")).thenReturn(testUser);
        when(jwtService.generateToken(testUser)).thenReturn("accessToken123");
        when(refreshSessionRepository.findByUserOrderByCreatedAtAsc(eq(testUser), any(PageRequest.class)))
                .thenReturn(oldSessions);
        when(request.getHeader("Fingerprint")).thenReturn("test-fingerprint-123");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        authenticationService.signIn(signInRequest, response, request);

        verify(refreshSessionRepository).deleteAll(oldSessions);
    }

    @Test
    @DisplayName("Должен обновить refresh token")
    void shouldRefreshToken() {
        when(refreshSessionRepository.findByRefreshToken(testRefreshToken))
                .thenReturn(Optional.of(testSession));
        when(request.getHeader("Fingerprint")).thenReturn("test-fingerprint-123");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(jwtService.generateToken(testUser)).thenReturn("newAccessToken123");

        AuthResponse result = authenticationService.refreshToken(testRefreshToken, response, request);

        assertThat(result.getAccessToken()).isEqualTo("newAccessToken123");
        verify(refreshSessionRepository).findByRefreshToken(testRefreshToken);
        verify(refreshSessionRepository).save(any(RefreshSession.class));
        verify(refreshSessionRepository).deleteByRefreshToken(testRefreshToken);
        verify(response).addCookie(any(Cookie.class));
    }

    @Test
    @DisplayName("Должен выбросить исключение при null токене")
    void shouldThrowExceptionWhenTokenIsNull() {
        assertThatThrownBy(() -> authenticationService.refreshToken(null, response, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Old token is null");

        verify(response).addCookie(any(Cookie.class));
    }

    @Test
    @DisplayName("Должен выбросить исключение при несуществующем токене")
    void shouldThrowExceptionWhenTokenNotFound() {
        UUID nonExistentToken = UUID.randomUUID();
        when(refreshSessionRepository.findByRefreshToken(nonExistentToken))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.refreshToken(nonExistentToken, response, request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Refresh token not found");

        verify(response).addCookie(any(Cookie.class));
    }

    @Test
    @DisplayName("Должен выбросить исключение при несовпадении fingerprint")
    void shouldThrowExceptionWhenFingerprintMismatch() {
        when(refreshSessionRepository.findByRefreshToken(testRefreshToken))
                .thenReturn(Optional.of(testSession));
        when(request.getHeader("Fingerprint")).thenReturn("wrong-fingerprint");

        assertThatThrownBy(() -> authenticationService.refreshToken(testRefreshToken, response, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Fingerprint mismatch");

        verify(response).addCookie(any(Cookie.class));
    }

    @Test
    @DisplayName("Должен выбросить исключение при истекшем токене")
    void shouldThrowExceptionWhenTokenExpired() {
        RefreshSession expiredSession = createExpiredRefreshSession(testUser, testRefreshToken, "test-fingerprint-123");
        when(refreshSessionRepository.findByRefreshToken(testRefreshToken))
                .thenReturn(Optional.of(expiredSession));
        when(request.getHeader("Fingerprint")).thenReturn("test-fingerprint-123");

        assertThatThrownBy(() -> authenticationService.refreshToken(testRefreshToken, response, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh expired");

        verify(response).addCookie(any(Cookie.class));
    }

    @Test
    @DisplayName("Должен создать refresh сессию с правильными параметрами")
    void shouldCreateRefreshSessionWithCorrectParameters() {
        when(userService.userDetailsService()).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(testUser);
        when(jwtService.generateToken(any())).thenReturn("accessToken");
        when(refreshSessionRepository.findByUserOrderByCreatedAtAsc(any(), any()))
                .thenReturn(Collections.emptyList());
        when(request.getHeader("Fingerprint")).thenReturn("test-fingerprint-123");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        authenticationService.signIn(signInRequest, response, request);

        ArgumentCaptor<RefreshSession> sessionCaptor = ArgumentCaptor.forClass(RefreshSession.class);
        verify(refreshSessionRepository).save(sessionCaptor.capture());

        RefreshSession capturedSession = sessionCaptor.getValue();
        assertThat(capturedSession.getUser()).isEqualTo(testUser);
        assertThat(capturedSession.getFingerprint()).isEqualTo("test-fingerprint-123");
        assertThat(capturedSession.getUa()).isEqualTo("Mozilla/5.0");
        assertThat(capturedSession.getIp()).isEqualTo("192.168.1.1");
        assertThat(capturedSession.getRefreshToken()).isNotNull();
    }

    @Test
    @DisplayName("Должен установить куку с правильными параметрами")
    void shouldSetCookieWithCorrectParameters() {
        when(userService.userDetailsService()).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(testUser);
        when(jwtService.generateToken(any())).thenReturn("accessToken");
        when(refreshSessionRepository.findByUserOrderByCreatedAtAsc(any(), any()))
                .thenReturn(Collections.emptyList());
        when(request.getHeader("Fingerprint")).thenReturn("fingerprint");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        authenticationService.signIn(signInRequest, response, request);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());

        Cookie capturedCookie = cookieCaptor.getValue();
        assertThat(capturedCookie.getName()).isEqualTo("refreshToken");
        assertThat(capturedCookie.getValue()).isNotBlank();
        assertThat(capturedCookie.isHttpOnly()).isTrue();
        assertThat(capturedCookie.getSecure()).isTrue();
        assertThat(capturedCookie.getPath()).isEqualTo("/api/auth");
        assertThat(capturedCookie.getMaxAge()).isEqualTo(60 * 60 * 24 * 30);
    }

    @Test
    @DisplayName("Должен очистить куку при ошибке")
    void shouldClearCookieOnError() {
        when(refreshSessionRepository.findByRefreshToken(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.refreshToken(UUID.randomUUID(), response, request))
                .isInstanceOf(NoSuchElementException.class);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());

        Cookie capturedCookie = cookieCaptor.getValue();
        assertThat(capturedCookie.getName()).isEqualTo("refreshToken");
        assertThat(capturedCookie.getValue()).isEmpty();
        assertThat(capturedCookie.getMaxAge()).isZero();
    }

}