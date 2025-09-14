package com.example.bankcards.util;

import com.example.bankcards.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class EncryptionUtil {

    private final CardRepository cardRepository;
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";

    @Value("${app.encryption.key}")
    private String encryptionKey;

    /**
     * Шифрует номер карты
     */
    public String encryptCardNumber(String cardNumber) {
        try {
            SecretKey secretKey = generateKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encryptedBytes = cipher.doFinal(cardNumber.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при шифровании номера карты", e);
        }
    }

    /**
     * Расшифровывает номер карты
     */
    public String decryptCardNumber(String encryptedCardNumber) {
        try {
            SecretKey secretKey = generateKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decodedBytes = Base64.getDecoder().decode(encryptedCardNumber);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при расшифровке номера карты", e);
        }
    }

    /**
     * Маскирует номер карты для отображения
     */
    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }

        String cleanNumber = cardNumber.replaceAll("\\s", "");
        if (cleanNumber.length() < 4) {
            return "****";
        }

        String lastFour = cleanNumber.substring(cleanNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }

    /**
     * Генерирует случайный номер карты
     */
    public String generateCardNumber() {
        SecureRandom random = new SecureRandom();
        StringBuilder cardNumber;
        do {
            cardNumber = new StringBuilder();

            cardNumber.append("4000");
            for (int i = 0; i < 8; i++) {
                cardNumber.append(random.nextInt(10));
            }

            for (int i = 0; i < 3; i++) {
                cardNumber.append(random.nextInt(10));
            }

            // Рассчитываем контрольную цифру по алгоритму Луна
            int checkDigit = calculateLuhnCheckDigit(cardNumber.toString());
            cardNumber.append(checkDigit);
        }while (Objects.equals(Boolean.FALSE,!isValidCardNumber(cardNumber.toString())));

        String encryptedCardNumber = encryptCardNumber(cardNumber.toString());

        if (cardRepository.existsByCardNumber(encryptedCardNumber)) return generateCardNumber();
        else return cardNumber.toString();
    }

    /**
     * Валидирует номер карты по алгоритму Луна
     */
    public boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 13 || cardNumber.length() > 19) {
            return false;
        }

        String cleanNumber = cardNumber.replaceAll("\\s", "");

        if (!cleanNumber.matches("\\d+")) {
            return false;
        }

        return isLuhnValid(cleanNumber);
    }

    private SecretKey generateKey() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
            byte[] key = new byte[16];
            System.arraycopy(keyBytes, 0, key, 0, 16);
            return new SecretKeySpec(key, ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при генерации ключа", e);
        }
    }

    private int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        boolean alternate = true;

        sum = getSum(number, sum, alternate);

        return (10 - (sum % 10)) % 10;
    }

    private int getSum(String number, int sum, boolean alternate) {
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }

            sum += digit;
            alternate = !alternate;
        }
        return sum;
    }

    private boolean isLuhnValid(String number) {
        int sum = 0;
        boolean alternate = false;

        sum = getSum(number, sum, alternate);

        return (sum % 10) == 0;
    }
}