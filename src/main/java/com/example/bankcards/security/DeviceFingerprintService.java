package com.example.bankcards.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Component
public class DeviceFingerprintService {

    public String generateFingerprint(HttpServletRequest request) {
        List<String> fingerprintComponents = new ArrayList<>();

        String clientIp = getClientIpAddress(request);
        fingerprintComponents.add("ip:" + clientIp);

        String userAgent = request.getHeader("User-Agent");
        if (StringUtils.hasText(userAgent)) {
            fingerprintComponents.add("ua:" + userAgent);
        }

        String acceptLanguage = request.getHeader("Accept-Language");
        if (StringUtils.hasText(acceptLanguage)) {
            fingerprintComponents.add("lang:" + acceptLanguage);
        }

        String acceptEncoding = request.getHeader("Accept-Encoding");
        if (StringUtils.hasText(acceptEncoding)) {
            fingerprintComponents.add("enc:" + acceptEncoding);
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            fingerprintComponents.add("xff:" + xForwardedFor);
        }

        fingerprintComponents.add("time:" + System.currentTimeMillis());

        String rawFingerprint = String.join("|", fingerprintComponents);

        return hashString(rawFingerprint);
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

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.substring(0, 16);

        } catch (NoSuchAlgorithmException e) {
            return "fp_" + Math.abs(input.hashCode());
        }
    }
}
