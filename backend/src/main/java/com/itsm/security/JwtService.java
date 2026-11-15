package com.itsm.security;

import com.itsm.domain.user.User;
import com.itsm.exception.AuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Issues and verifies signed JSON Web Tokens (HMAC-SHA256) used for stateless authentication. */
@Component
public class JwtService {

    private final SecretKey key;
    private final Duration validity;

    public JwtService(@Value("${itsm.jwt.secret}") String secret,
                      @Value("${itsm.jwt.validity-hours:8}") long validityHours) {
        if (secret.length() < 32) {
            throw new IllegalStateException("itsm.jwt.secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validity = Duration.ofHours(validityHours);
    }

    public String issueToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(key)
                .compact();
    }

    /** @return the user id stored in a valid token */
    public Long verify(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException ex) {
            throw AuthenticationException.invalidToken();
        }
    }
}
