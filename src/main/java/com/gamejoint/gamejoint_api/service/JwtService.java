package com.gamejoint.gamejoint_api.service;

import com.gamejoint.gamejoint_api.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    /**
     * Creates the Token for the mobile app
     */
    public String generateToken(User user) {
        
        // We pack extra, non-sensitive data into the token payload
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        
        // 1. Pack the Role ID so the frontend knows if they are a standard user or staff
        if (user.getRole() != null) {
            extraClaims.put("roleId", user.getRole().getId());
        }

        // 2. Pack the Ban Status to instantly trigger the frontend UI locks
        extraClaims.put("isBanned", user.getIsBanned() != null ? user.getIsBanned() : false);
        
        // 3. Pack the Ban Expiration Time for the modal text
        if (user.getBanExpiresAt() != null) {
            extraClaims.put("banExpiration", user.getBanExpiresAt().toString());
        } else {
            extraClaims.put("banExpiration", "Permanent");
        }
        
        extraClaims.put("tokenVersion", user.getTokenVersion());

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the Username from an incoming token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Checks if the token belongs to the user and is not expired
     */
    public boolean isTokenValid(String token, User user) {
        final String username = extractUsername(token);
        return (username.equals(user.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Integer extractTokenVersion(String token) {
        return extractClaim(token, claims -> claims.get("tokenVersion", Integer.class));
    }
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}