package com.example.bankcards.security;

import com.example.bankcards.repositories.CardRepository;
import com.example.bankcards.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты шифрования номеров карт (без Spring)")
class EncryptionUtilJUnitTest {

    @Mock
    private CardRepository cardRepository;

    private EncryptionUtil encryptionUtil;

    @BeforeEach
    void setUp() {
        encryptionUtil = new EncryptionUtil(cardRepository);
        ReflectionTestUtils.setField(encryptionUtil, "encryptionKey",
                "cd0211c7721d48006aff56996fe13e33d2b4924672b3a6aa2d2bf7786bfa0fee");
    }

    @Test
    @DisplayName("Должен корректно шифровать и расшифровывать номер карты")
    public void testCardNumberEncryption() {
        String originalCardNumber = "4000123456789012";

        String encrypted = encryptionUtil.encryptCardNumber(originalCardNumber);
        System.out.println("Original: " + originalCardNumber);
        System.out.println("Encrypted: " + encrypted);

        String decrypted = encryptionUtil.decryptCardNumber(encrypted);
        System.out.println("Decrypted: " + decrypted);

        assertThat(encrypted).isNotEqualTo(originalCardNumber);
        assertThat(decrypted).isEqualTo(originalCardNumber);
        assertThat(encrypted).matches("^[A-Za-z0-9+/]*={0,2}$");
    }

    @Test
    @DisplayName("Должен генерировать валидный номер карты")
    public void testCardNumberGeneration() {
        when(cardRepository.existsByCardNumber(any())).thenReturn(false);

        String generatedCardNumber = encryptionUtil.generateCardNumber();
        System.out.println("Generated card number: " + generatedCardNumber);

        assertThat(generatedCardNumber).isNotNull();
        assertThat(generatedCardNumber).hasSize(16);
        assertThat(generatedCardNumber).matches("\\d{16}");
        assertThat(encryptionUtil.isValidCardNumber(generatedCardNumber)).isTrue();
    }

    @Test
    @DisplayName("Должен корректно маскировать номер карты")
    public void testCardMasking() {
        String card1 = "1234567890123456";
        String masked1 = encryptionUtil.maskCardNumber(card1);
        assertThat(masked1).isEqualTo("**** **** **** 3456");

        String card2 = "1234 5678 9012 3456";
        String masked2 = encryptionUtil.maskCardNumber(card2);
        assertThat(masked2).isEqualTo("**** **** **** 3456");

        String card3 = "123";
        String masked3 = encryptionUtil.maskCardNumber(card3);
        assertThat(masked3).isEqualTo("****");

        String masked4 = encryptionUtil.maskCardNumber(null);
        assertThat(masked4).isEqualTo("****");
    }

    @Test
    @DisplayName("Должен валидировать номера карт по алгоритму Луна")
    public void testCardNumberValidation() {
        assertThat(encryptionUtil.isValidCardNumber("4000000000000002")).isTrue();
        assertThat(encryptionUtil.isValidCardNumber("5555555555554444")).isTrue();

        assertThat(encryptionUtil.isValidCardNumber("4000000000000001")).isFalse();
        assertThat(encryptionUtil.isValidCardNumber("123")).isFalse();
        assertThat(encryptionUtil.isValidCardNumber("12345678901234567890")).isFalse();
        assertThat(encryptionUtil.isValidCardNumber("abcd1234efgh5678")).isFalse();
        assertThat(encryptionUtil.isValidCardNumber(null)).isFalse();
    }
}