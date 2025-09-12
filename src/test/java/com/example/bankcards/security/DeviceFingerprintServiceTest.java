package com.example.bankcards.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты сервиса генерации отпечатка устройства")
class DeviceFingerprintServiceTest {

    @InjectMocks
    private DeviceFingerprintService deviceFingerprintService;

    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockRequest = new MockHttpServletRequest();
    }

    @Test
    @DisplayName("Должен генерировать fingerprint с базовыми заголовками")
    void shouldGenerateFingerprintWithBasicHeaders() {
        mockRequest.setRemoteAddr("192.168.1.100");
        mockRequest.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        mockRequest.addHeader("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8");
        mockRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

        String fingerprint = deviceFingerprintService.generateFingerprint(mockRequest);

        assertThat(fingerprint).isNotNull();
        assertThat(fingerprint).hasSize(16); // Первые 16 символов SHA-256
        assertThat(fingerprint).matches("^[a-f0-9]{16}$"); // Только hex символы
    }

    @Test
    @DisplayName("Должен обрабатывать X-Forwarded-For заголовок")
    void shouldHandleXForwardedForHeader() {
        mockRequest.setRemoteAddr("10.0.0.1");
        mockRequest.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178");
        mockRequest.addHeader("User-Agent", "Mozilla/5.0");

        String fingerprint = deviceFingerprintService.generateFingerprint(mockRequest);

        assertThat(fingerprint).isNotNull();
        assertThat(fingerprint).hasSize(16);
    }

    @Test
    @DisplayName("Должен обрабатывать X-Real-IP заголовок")
    void shouldHandleXRealIPHeader() {
        mockRequest.setRemoteAddr("10.0.0.1");
        mockRequest.addHeader("X-Real-IP", "203.0.113.195");
        mockRequest.addHeader("User-Agent", "Mozilla/5.0");

        String fingerprint = deviceFingerprintService.generateFingerprint(mockRequest);

        assertThat(fingerprint).isNotNull();
        assertThat(fingerprint).hasSize(16);
    }

    @Test
    @DisplayName("Должен генерировать разные fingerprints для разных запросов")
    void shouldGenerateDifferentFingerprintsForDifferentRequests() {
        // Первый запрос
        mockRequest.setRemoteAddr("192.168.1.100");
        mockRequest.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        String fingerprint1 = deviceFingerprintService.generateFingerprint(mockRequest);

        // Второй запрос с другими данными
        MockHttpServletRequest mockRequest2 = new MockHttpServletRequest();
        mockRequest2.setRemoteAddr("10.0.0.1");
        mockRequest2.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)");
        String fingerprint2 = deviceFingerprintService.generateFingerprint(mockRequest2);

        assertThat(fingerprint1).isNotEqualTo(fingerprint2);
    }

    @Test
    @DisplayName("Должен генерировать уникальные fingerprints даже для похожих запросов")
    void shouldGenerateUniqueFingerprintsForSimilarRequests() throws InterruptedException {
        mockRequest.setRemoteAddr("192.168.1.100");
        mockRequest.addHeader("User-Agent", "Mozilla/5.0");

        String fingerprint1 = deviceFingerprintService.generateFingerprint(mockRequest);

        // Небольшая задержка чтобы изменилась временная метка
        Thread.sleep(1);

        String fingerprint2 = deviceFingerprintService.generateFingerprint(mockRequest);

        // Должны быть разными из-за временной метки
        assertThat(fingerprint1).isNotEqualTo(fingerprint2);
    }

    @Test
    @DisplayName("Должен работать с минимальной информацией")
    void shouldWorkWithMinimalInformation() {
        mockRequest.setRemoteAddr("127.0.0.1");
        // Не добавляем никаких заголовков

        String fingerprint = deviceFingerprintService.generateFingerprint(mockRequest);

        assertThat(fingerprint).isNotNull();
        assertThat(fingerprint).hasSize(16);
        assertThat(fingerprint).matches("^[a-f0-9]{16}$");
    }


    @Test
    @DisplayName("Должен обрабатывать специальные символы в заголовках")
    void shouldHandleSpecialCharactersInHeaders() {
        mockRequest.setRemoteAddr("192.168.1.1");
        mockRequest.addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:91.0) Gecko/20100101 Firefox/91.0");
        mockRequest.addHeader("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3");
        mockRequest.addHeader("Accept-Encoding", "gzip, deflate, br");

        String fingerprint = deviceFingerprintService.generateFingerprint(mockRequest);

        assertThat(fingerprint).isNotNull();
        assertThat(fingerprint).hasSize(16);
        assertThat(fingerprint).matches("^[a-f0-9]{16}$");
    }

    @Test
    @DisplayName("Должен приоритизировать X-Forwarded-For над X-Real-IP")
    void shouldPrioritizeXForwardedForOverXRealIP() {
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.setRemoteAddr("10.0.0.1");
        request1.addHeader("X-Forwarded-For", "203.0.113.195");
        request1.addHeader("X-Real-IP", "198.51.100.1");
        request1.addHeader("User-Agent", "Mozilla/5.0");

        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.setRemoteAddr("10.0.0.1");
        request2.addHeader("X-Real-IP", "198.51.100.1");
        request2.addHeader("User-Agent", "Mozilla/5.0");

        String fingerprint1 = deviceFingerprintService.generateFingerprint(request1);
        String fingerprint2 = deviceFingerprintService.generateFingerprint(request2);

        // Fingerprints должны отличаться, так как разные IP будут использованы
        assertThat(fingerprint1).isNotEqualTo(fingerprint2);
    }

    @Test
    @DisplayName("Должен обрабатывать IPv6 адреса")
    void shouldHandleIPv6Addresses() {
        mockRequest.setRemoteAddr("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        mockRequest.addHeader("User-Agent", "Mozilla/5.0");

        String fingerprint = deviceFingerprintService.generateFingerprint(mockRequest);

        assertThat(fingerprint).isNotNull();
        assertThat(fingerprint).hasSize(16);
        assertThat(fingerprint).matches("^[a-f0-9]{16}$");
    }
}
