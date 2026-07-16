package com.school.security.securities.services;

import java.util.HashMap;
import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {
    String generateToken(UserDetails userDetails);

    public String extractUsername(String token);

    boolean isTokenValid(String token, UserDetails userDetails);

    String generateRefreshToken(
            HashMap<String, Object> objectObjectHashMap, UserDetails userDetails);
}
