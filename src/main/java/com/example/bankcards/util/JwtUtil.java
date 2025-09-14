package com.example.bankcards.util;

import com.example.bankcards.repositories.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;
    private final UserRepository repository;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }


    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }


    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        if (userDetails instanceof com.example.bankcards.entity.User user) {
            claims.put("userId", user.getId());
            claims.put("firstName", user.getFirstName());
            claims.put("middleName", user.getMiddleName());
            claims.put("lastName", user.getLastName());
        }
        long jwtExpiration = 5 * 24 * 60 * 60 * 1000L;
        return createToken(claims, userDetails.getUsername(), jwtExpiration);
    }

    public Long extractUserId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object userIdClaim = claims.get("userId");

            if (userIdClaim instanceof Integer) {
                return ((Integer) userIdClaim).longValue();
            } else if (userIdClaim instanceof Long) {
                return (Long) userIdClaim;
            }
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }


    public Boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractFullName(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object firstName = claims.get("firstName");
            Object lastName = claims.get("lastName");
            Object middleName = claims.get("middleName");

            String firstNameStr = (firstName instanceof String) ? (String) firstName : "";
            String lastNameStr = (lastName instanceof String) ? (String) lastName : "";
            String middleNameStr = (middleName instanceof String) ? (String) middleName : "";

            StringBuilder fullName = new StringBuilder();

            if (!lastNameStr.isEmpty()) {
                fullName.append(lastNameStr);
            }

            if (!firstNameStr.isEmpty()) {
                if (fullName.length() > 0) fullName.append(" ");
                fullName.append(firstNameStr);
            }

            if (!middleNameStr.isEmpty()) {
                if (fullName.length() > 0) fullName.append(" ");
                fullName.append(middleNameStr);
            }

            return fullName.length() > 0 ? fullName.toString() : null;

        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username != null &&
                   username.equals(userDetails.getUsername()) &&
                   !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}