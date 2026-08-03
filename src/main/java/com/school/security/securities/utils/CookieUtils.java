package com.school.security.securities.utils;
import jakarta.servlet.http.Cookie;

public class CookieUtils {

    private CookieUtils() {
    }

    public static Cookie createAccessTokenCookie(String token) {

        Cookie cookie = new Cookie("accessToken", token);

        cookie.setHttpOnly(true);
        cookie.setSecure(false); // true en production HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(15 * 60);

        return cookie;
    }

    public static Cookie createRefreshTokenCookie(String token) {

        Cookie cookie = new Cookie("refreshToken", token);

        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);

        return cookie;
    }

    public static Cookie deleteAccessTokenCookie() {

        Cookie cookie = new Cookie("accessToken", "");

        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        return cookie;
    }

    public static Cookie deleteRefreshTokenCookie() {

        Cookie cookie = new Cookie("refreshToken", "");

        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        return cookie;
    }
}