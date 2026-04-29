package com.pythonadmin.security;

import com.pythonadmin.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HexFormat;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final AppProperties props;
    private final SecureRandom random = new SecureRandom();

    public JwtService(AppProperties props) {
        this.props = props;
    }

    public String createToken(String subject) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.jwt().expireMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
            .subject(subject)
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .claim("jti", HexFormat.of().formatHex(randomBytes(16)))
            .signWith(secretKey())
            .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser().verifyWith(secretKey()).build().parseSignedClaims(token);
    }

    private SecretKey secretKey() {
        byte[] raw = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        byte[] key = sha256(raw);
        return Keys.hmacShaKeyFor(key);
    }

    private static byte[] sha256(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input);
        } catch (Exception e) {
            throw new IllegalStateException("sha256 failed", e);
        }
    }

    private byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        random.nextBytes(b);
        return b;
    }
}
