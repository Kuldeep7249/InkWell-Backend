package com.inkwell.notification.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Objects;

@Component
public class JwtUtil {

    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret}") String jwtSecret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public UserPrincipal buildPrincipal(String token) {
        Claims claims = extractAllClaims(token);

        Long userId = claims.get("userId", Number.class).longValue();
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);

        if (Objects.isNull(userId) || Objects.isNull(username) || Objects.isNull(role)) {
            throw new IllegalArgumentException("Token is missing required claims");
        }

        return UserPrincipal.builder()
                .userId(userId)
                .username(username)
                .role(role)
                .build();
    }

    public boolean isTokenValid(String token) {
        extractAllClaims(token);
        return true;
    }
}
