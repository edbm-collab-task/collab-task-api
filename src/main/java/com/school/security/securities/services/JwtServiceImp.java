package com.school.security.securities.services;

import com.school.security.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtServiceImp implements JwtService {
    @Value("${jwt.secret.key}")
    private String secretKey;

    @Override
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", userDetails.getAuthorities())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(getSiginKey())
                .compact();
    }

    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsTFunction) {
        final Claims claims = extractAllClaims(token);
        return claimsTFunction.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
    }

    private Key getSiginKey() {
        byte[] key = Decoders.BASE64.decode(this.secretKey);
        return Keys.hmacShaKeyFor(key);
    }

    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    @Override
    public String generateRefreshToken(
            HashMap<String, Object> extractClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extractClaims)
                .claim("roles",
                        userDetails.getAuthorities()
                                .stream()
                                .map(GrantedAuthority::getAuthority)
                                .toList()
                )
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 604800000))
                .signWith(getSiginKey())
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    @Override
    public String generateRecoveryToken(User user, int code) {

        Map<String, Object> claims = new HashMap<>();

        claims.put("code", code);
        claims.put("type", "RECOVERY");

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
                .signWith(getSiginKey())
                .compact();
    }

    @Override
    public int extractRecoveryCode(String token) {
        return extractAllClaims(token)
                .get("code", Integer.class);
    }

    @Override
    public boolean isRecoveryTokenValid(String token) {

        try {

            Claims claims = extractAllClaims(token);

            String type = claims.get("type", String.class);

            return "RECOVERY".equals(type)
                    && !isTokenExpired(token);

        } catch (Exception e) {

            return false;

        }
    }

}
