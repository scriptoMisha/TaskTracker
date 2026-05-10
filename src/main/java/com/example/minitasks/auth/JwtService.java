package com.example.minitasks.auth;

import com.example.minitasks.config.JwtProperties;
import com.example.minitasks.user.Role;
import com.example.minitasks.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccess(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(props.issuer())
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.accessTtl())))
                .signWith(key)
                .compact();
    }

    public String issueRefresh(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(props.issuer())
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.refreshTtl())))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public ParsedAccess parseAccess(String token) {
        Claims c = parse(token);
        if (!"access".equals(c.get("type", String.class))) {
            throw new io.jsonwebtoken.JwtException("Not an access token");
        }
        return new ParsedAccess(
                UUID.fromString(c.getSubject()),
                c.get("email", String.class),
                Role.valueOf(c.get("role", String.class))
        );
    }

    public Instant refreshExpiry(String refreshToken) {
        return parse(refreshToken).getExpiration().toInstant();
    }

    public UUID userIdFromRefresh(String refreshToken) {
        Claims c = parse(refreshToken);
        if (!"refresh".equals(c.get("type", String.class))) {
            throw new io.jsonwebtoken.JwtException("Not a refresh token");
        }
        return UUID.fromString(c.getSubject());
    }

    public record ParsedAccess(UUID userId, String email, Role role) {
    }
}
