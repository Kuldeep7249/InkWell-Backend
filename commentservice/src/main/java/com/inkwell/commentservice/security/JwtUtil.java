package com.inkwell.commentservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Component
public class JwtUtil {

    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret}") String jwtSecret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UserPrincipal buildPrincipal(String token) {
        Claims claims = extractAllClaims(token);

        Long userId = claims.get("userId", Number.class).longValue();
        String username = claims.get("username", String.class);

        List<String> roles = new ArrayList<>();
        Object roleClaim = claims.get("role");
        if (roleClaim instanceof String roleString && !roleString.isBlank()) {
            roles.add(normalizeRole(roleString));
        }

        Object rolesClaim = claims.get("roles");
        if (rolesClaim instanceof Collection<?> collection) {
            collection.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .map(this::normalizeRole)
                    .forEach(role -> {
                        if (!roles.contains(role)) {
                            roles.add(role);
                        }
                    });
        }

        if (roles.isEmpty()) {
            roles.add("ROLE_READER");
        }

        return new UserPrincipal(userId, username, roles);
    }

    private String normalizeRole(String role) {
        return role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
    }
}
