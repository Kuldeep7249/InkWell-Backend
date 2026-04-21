package com.inkwell.categoryservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtUtil {

    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret}") String jwtSecret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public UserPrincipal buildPrincipal(String token) {
        Claims claims = extractAllClaims(token);

        Long userId = claims.get("userId", Number.class).longValue();
        String username = claims.get("username", String.class);

        List<String> roles = new ArrayList<>();
        Object roleClaim = claims.get("role");
        Object rolesClaim = claims.get("roles");

        if (rolesClaim instanceof List<?> roleList) {
            roleList.forEach(role -> roles.add(String.valueOf(role)));
        } else if (roleClaim != null) {
            roles.add(String.valueOf(roleClaim));
        }

        if (roles.isEmpty()) {
            roles.add("READER");
        }

        return UserPrincipal.of(userId, username, roles);
    }
}
