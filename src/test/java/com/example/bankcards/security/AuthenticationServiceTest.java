package com.example.bankcards.security;

import com.example.bankcards.dto.users.SignInRequest;
import com.example.bankcards.dto.users.SignUpRequest;
import com.example.bankcards.entity.RefreshSession;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repositories.RefreshSessionRepository;
import com.example.bankcards.repositories.UserRepository;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtService;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshSessionRepository refreshSessionRepository;

    @Mock
    private DeviceFingerprintService deviceFingerprintService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private SignUpRequest signUpRequest;
    private SignInRequest signInRequest;
    private RefreshSession testSession;
    private UUID testRefreshToken;
    private MockHttpServletRequest mockRequest;
    private MockHttpServletResponse mockResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .firstName("Тест")
                .lastName("Тестов")
                .middleName("Тестович")
                .phoneNumber("500123321")
                .password("hashed123")
                .role(new Role(2L,"ADMIN","KUKU",null))
                .enabled(true)
                .build();

        signUpRequest = new SignUpRequest();
        signUpRequest.setName("Тест");
        signUpRequest.setSurname("Тестов");
        signUpRequest.setMiddleName("Тестович");
        signUpRequest.setPhoneNumber("500123321");
        signUpRequest.setPassword("qwe");

        signInRequest = new SignInRequest();
        signInRequest.setPhoneNumber("500123321");
        signInRequest.setPassword("qwe");

        testRefreshToken = UUID.randomUUID();
        testSession = createValidRefreshSession(testUser, testRefreshToken, "auto-generated-fingerprint");

        mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        mockRequest.addHeader("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8");
        mockRequest.setRemoteAddr("192.168.1.100");

        mockResponse = new MockHttpServletResponse();
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
    @DisplayName("Должен выполнить вход пользователя с автогенерацией fingerprint")
    void shouldSignInUser() {
        when(userService.userDetailsService()).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername("500123321")).thenReturn(testUser);
        when(jwtService.generateToken(testUser)).thenReturn("accessToken123");
        when(refreshSessionRepository.findByUserOrderByCreatedAtAsc(eq(testUser), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());
        when(deviceFingerprintService.generateFingerprint(any(HttpServletRequest.class)))
                .thenReturn("auto-generated-fingerprint-123");

        String result = authenticationService.signIn(signInRequest, mockResponse, mockRequest);

        assertThat(result).isEqualTo("accessToken123");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(deviceFingerprintService).generateFingerprint(mockRequest);
        verify(refreshSessionRepository).save(any(RefreshSession.class));


        Cookie[] cookies = mockResponse.getCookies();
        assertThat(cookies).hasSize(1);
        assertThat(cookies[0].getName()).isEqualTo("refreshToken");
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
        when(userDetailsService.loadUserByUsername("500123321")).thenReturn(testUser);
        when(jwtService.generateToken(testUser)).thenReturn("accessToken123");
        when(refreshSessionRepository.findByUserOrderByCreatedAtAsc(eq(testUser), any(PageRequest.class)))
                .thenReturn(oldSessions);
        when(deviceFingerprintService.generateFingerprint(any(HttpServletRequest.class)))
                .thenReturn("auto-generated-fingerprint-123");

        authenticationService.signIn(signInRequest, mockResponse, mockRequest);

        verify(refreshSessionRepository).deleteAll(oldSessions);
    }

    @Test
    @DisplayName("Должен обновить refresh token")
    void shouldRefreshToken() {
        when(refreshSessionRepository.findByRefreshToken(testRefreshToken))
                .thenReturn(Optional.of(testSession));
        when(deviceFingerprintService.generateFingerprint(any(HttpServletRequest.class)))
                .thenReturn("auto-generated-fingerprint");
        when(jwtService.generateToken(testUser)).thenReturn("newAccessToken123");

        String result = authenticationService.refreshToken(testRefreshToken, mockResponse, mockRequest);

        assertThat(result).isEqualTo("newAccessToken123");
        verify(refreshSessionRepository).findByRefreshToken(testRefreshToken);
        verify(refreshSessionRepository).save(any(RefreshSession.class));
        verify(refreshSessionRepository).deleteByRefreshToken(testRefreshToken);
        verify(deviceFingerprintService).generateFingerprint(mockRequest);

        Cookie[] cookies = mockResponse.getCookies();
        assertThat(cookies).hasSize(1);
        assertThat(cookies[0].getName()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("Должен выбросить исключение при null токене")
    void shouldThrowExceptionWhenTokenIsNull() {
        assertThatThrownBy(() -> authenticationService.refreshToken(null, mockResponse, mockRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Old token is null");

        Cookie[] cookies = mockResponse.getCookies();
        assertThat(cookies).hasSize(1);
        assertThat(cookies[0].getName()).isEqualTo("refreshToken");
        assertThat(cookies[0].getValue()).isEmpty();
        assertThat(cookies[0].getMaxAge()).isZero();
    }

    @Test
    @DisplayName("Должен выбросить исключение при несуществующем токене")
    void shouldThrowExceptionWhenTokenNotFound() {
        UUID nonExistentToken = UUID.randomUUID();
        when(refreshSessionRepository.findByRefreshToken(nonExistentToken))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.refreshToken(nonExistentToken, mockResponse, mockRequest))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Refresh token not found");

        Cookie[] cookies = mockResponse.getCookies();
        assertThat(cookies).hasSize(1);
        assertThat(cookies[0].getName()).isEqualTo("refreshToken");
        assertThat(cookies[0].getValue()).isEmpty();
        assertThat(cookies[0].getMaxAge()).isZero();
    }

    @Test
    @DisplayName("Должен выбросить исключение при истекшем токене")
    void shouldThrowExceptionWhenTokenExpired() {
        RefreshSession expiredSession = createExpiredRefreshSession(testUser, testRefreshToken, "auto-generated-fingerprint");
        when(refreshSessionRepository.findByRefreshToken(testRefreshToken))
                .thenReturn(Optional.of(expiredSession));
        when(deviceFingerprintService.generateFingerprint(any(HttpServletRequest.class)))
                .thenReturn("auto-generated-fingerprint");

        assertThatThrownBy(() -> authenticationService.refreshToken(testRefreshToken, mockResponse, mockRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh token expired");

        Cookie[] cookies = mockResponse.getCookies();
        assertThat(cookies).hasSize(1);
        assertThat(cookies[0].getName()).isEqualTo("refreshToken");
        assertThat(cookies[0].getValue()).isEmpty();
        assertThat(cookies[0].getMaxAge()).isZero();
    }

    @Test
    @DisplayName("Должен создать refresh сессию с автогенерированным fingerprint")
    void shouldCreateRefreshSessionWithAutoGeneratedFingerprint() {
        when(userService.userDetailsService()).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(testUser);
        when(jwtService.generateToken(any())).thenReturn("accessToken");
        when(refreshSessionRepository.findByUserOrderByCreatedAtAsc(any(), any()))
                .thenReturn(Collections.emptyList());
        when(deviceFingerprintService.generateFingerprint(any(HttpServletRequest.class)))
                .thenReturn("auto-generated-fingerprint-from-headers");

        authenticationService.signIn(signInRequest, mockResponse, mockRequest);

        ArgumentCaptor<RefreshSession> sessionCaptor = ArgumentCaptor.forClass(RefreshSession.class);
        verify(refreshSessionRepository).save(sessionCaptor.capture());

        RefreshSession capturedSession = sessionCaptor.getValue();
        assertThat(capturedSession.getUser()).isEqualTo(testUser);
        assertThat(capturedSession.getFingerprint()).isEqualTo("auto-generated-fingerprint-from-headers");
        assertThat(capturedSession.getUa()).isEqualTo("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        assertThat(capturedSession.getIp()).isEqualTo("192.168.1.100");
        assertThat(capturedSession.getRefreshToken()).isNotNull();

        verify(deviceFingerprintService).generateFingerprint(mockRequest);
    }

    @Test
    @DisplayName("Должен установить куку с правильными параметрами")
    void shouldSetCookieWithCorrectParameters() {
        when(userService.userDetailsService()).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(testUser);
        when(jwtService.generateToken(any())).thenReturn("accessToken");
        when(refreshSessionRepository.findByUserOrderByCreatedAtAsc(any(), any()))
                .thenReturn(Collections.emptyList());
        when(deviceFingerprintService.generateFingerprint(any(HttpServletRequest.class)))
                .thenReturn("fingerprint-for-cookie-test");

        authenticationService.signIn(signInRequest, mockResponse, mockRequest);

        Cookie[] cookies = mockResponse.getCookies();
        assertThat(cookies).hasSize(1);

        Cookie capturedCookie = cookies[0];
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

        assertThatThrownBy(() -> authenticationService.refreshToken(UUID.randomUUID(), mockResponse, mockRequest))
                .isInstanceOf(NoSuchElementException.class);

        Cookie[] cookies = mockResponse.getCookies();
        assertThat(cookies).hasSize(1);

        Cookie capturedCookie = cookies[0];
        assertThat(capturedCookie.getName()).isEqualTo("refreshToken");
        assertThat(capturedCookie.getValue()).isEmpty();
        assertThat(capturedCookie.getMaxAge()).isZero();
    }

    @Test
    @DisplayName("Должен обрабатывать запросы с X-Forwarded-For заголовком")
    void shouldHandleXForwardedForHeader() {
        mockRequest.addHeader("X-Forwarded-For", "10.0.0.1, 192.168.1.1");

        when(userService.userDetailsService()).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(testUser);
        when(jwtService.generateToken(any())).thenReturn("accessToken");
        when(refreshSessionRepository.findByUserOrderByCreatedAtAsc(any(), any()))
                .thenReturn(Collections.emptyList());
        when(deviceFingerprintService.generateFingerprint(any(HttpServletRequest.class)))
                .thenReturn("fingerprint-with-forwarded-ip");

        authenticationService.signIn(signInRequest, mockResponse, mockRequest);

        ArgumentCaptor<RefreshSession> sessionCaptor = ArgumentCaptor.forClass(RefreshSession.class);
        verify(refreshSessionRepository).save(sessionCaptor.capture());

        RefreshSession capturedSession = sessionCaptor.getValue();
        assertThat(capturedSession.getIp()).isEqualTo("10.0.0.1"); // Первый IP из X-Forwarded-For
        verify(deviceFingerprintService).generateFingerprint(mockRequest);
    }

    @Test
    @DisplayName("DeviceFingerprintService должен вызываться для каждого запроса")
    void shouldCallDeviceFingerprintServiceForEachRequest() {
        when(userService.userDetailsService()).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(testUser);
        when(jwtService.generateToken(any())).thenReturn("accessToken");
        when(refreshSessionRepository.findByUserOrderByCreatedAtAsc(any(), any()))
                .thenReturn(Collections.emptyList());
        when(deviceFingerprintService.generateFingerprint(any(HttpServletRequest.class)))
                .thenReturn("unique-fingerprint-123");

        authenticationService.signIn(signInRequest, mockResponse, mockRequest);

        verify(deviceFingerprintService, Mockito.times(1)).generateFingerprint(mockRequest);
    }
}