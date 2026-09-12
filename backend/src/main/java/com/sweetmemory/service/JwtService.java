package com.sweetmemory.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.sweetmemory.config.AppProperties;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT 签发与校验（jjwt 0.12 API）
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final int expireDays;

    public JwtService(AppProperties props) {
        byte[] secret = props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("app.jwt.secret 长度必须 ≥ 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.expireDays = props.getJwt().getExpireDays();
    }

    public String issue(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plus(expireDays, ChronoUnit.DAYS)))
                .signWith(key)
                .compact();
    }

    /** 校验通过返回用户名，失败返回 null */
    public String verify(String token) {
        try {
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}
