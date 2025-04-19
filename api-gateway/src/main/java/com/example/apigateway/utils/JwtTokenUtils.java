package com.example.apigateway.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtTokenUtils {

    @Value("${jwt.secret}")
    private String secret;

    public String getUsername(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public List<String> getRoles(String token) {
        Object roles = getClaimsFromToken(token).get("roles");

        if (roles instanceof List<?> roleList) {
            return roleList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        } else {
            throw new IllegalArgumentException("Invalid roles format in JWT");
        }
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
