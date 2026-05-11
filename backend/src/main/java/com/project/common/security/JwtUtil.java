package com.project.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  public static final String CLAIM_ROLE = "role";

  private final Key key;
  private final long expirationMs;

  public JwtUtil(JwtProperties properties) {
    this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    this.expirationMs = properties.getExpirationMs();
  }

  public String generateToken(String username, String role) {
    return generateToken(username, Map.of(CLAIM_ROLE, role));
  }

  public String generateToken(String subject, Map<String, Object> claims) {
    long now = System.currentTimeMillis();
    return Jwts.builder()
        .setSubject(subject)
        .addClaims(claims)
        .setIssuedAt(new Date(now))
        .setExpiration(new Date(now + expirationMs))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  public Claims parseToken(String token) {
    return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
  }

  public boolean validateToken(String token) {
    try {
      parseToken(token);
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }
}

