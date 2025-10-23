package com.marketplace.core.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {
	
	@Value("${app.jwt.secret}")
    private String jwtSecret;
	
	@Value("${app.jwt.expiration-ms:86400000}") //24h
    private long jwtExpirationMs;
	
	private Key getKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

	public String generateToken(String username) {
	    Date now = new Date();
	    Date expiry = new Date(now.getTime() + jwtExpirationMs);
	    return Jwts.builder()
	            .setSubject(username)
	            .setIssuedAt(now)
	            .setExpiration(expiry)
	            .signWith(getKey()) // pas de SignatureAlgorithm en 0.11.x
	            .compact();
	}
	
	// Pour parser un token :
	public String getUsernameFromToken(String token) {
	    return Jwts.parserBuilder()
	            .setSigningKey(getKey())
	            .build()
	            .parseClaimsJws(token)
	            .getBody()
	            .getSubject();
	}

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Token invalide ou expiré
            return false;
        }
    }
}
