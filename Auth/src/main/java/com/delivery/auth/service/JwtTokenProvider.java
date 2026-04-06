package com.delivery.auth.service;

import com.delivery.auth.domain.user.AuthUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    private final SecretKey secretKey;
    @Getter
    private final long accessTokenExpirationSeconds;
    private final String issuer;

    public JwtTokenProvider(
        @Value("${auth.jwt.secret}") String jwtSecret,
        @Value("${auth.jwt.access-token-expiration-seconds:1800}") long accessTokenExpirationSeconds,
        @Value("${auth.jwt.issuer:aekiori-eats}") String issuer
    ) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.issuer = issuer;
    }

    public String createAccessToken(AuthUser authUser) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenExpirationSeconds, ChronoUnit.SECONDS);

        return Jwts.builder()
            .subject(String.valueOf(authUser.getUserId()))
            .claim("user_id", authUser.getUserId())
            .claim("email", authUser.getEmail())
            .claim("nickname", authUser.getNickname())
            .claim("role", authUser.getRole())
            .claim("type", "access")
            .issuer(issuer)
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(secretKey)
            .compact();
    }

}
