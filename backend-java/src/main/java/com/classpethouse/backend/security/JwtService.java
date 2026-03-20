package com.classpethouse.backend.security;

import com.classpethouse.backend.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;

    public JwtService(Environment environment) {
        String rawSecret = environment.getProperty("JWT_SECRET", "class-pet-house-secret-class-pet-house-secret");
        if (rawSecret.length() < 32) {
            rawSecret = (rawSecret + "class-pet-house-secret-class-pet-house-secret").substring(0, 32);
        }
        this.secretKey = Keys.hmacShaKeyFor(rawSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserEntity user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("id", user.getId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(7, ChronoUnit.DAYS)))
                .signWith(secretKey)
                .compact();
    }

    public Integer parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Number id = claims.get("id", Number.class);
        return id == null ? null : id.intValue();
    }
}
