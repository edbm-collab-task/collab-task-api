package com.school.security.securities.utils;
import jakarta.servlet.http.Cookie;

public class CookieUtils {

    private CookieUtils() {
    }

    public static Cookie createEmail(String email) {

        Cookie cookie = new Cookie("email", email);

        cookie.setHttpOnly(true);
        cookie.setSecure(false); // true en production HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(15 * 60);

        return cookie;
    }

    public static  Cookie deleteEmail(){
        Cookie cookie = new Cookie("email", "");

        cookie.setHttpOnly(true);
        cookie.setSecure(false); // true en production HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(15 * 60);

        return cookie;
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

    public static Cookie createRecoveryTokenCookie(String token) {

        Cookie cookie = new Cookie("recoveryToken", token);

        cookie.setHttpOnly(true);
        cookie.setSecure(false); // true en production HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(10 * 60); // 10 minutes

        return cookie;
    }

    public static Cookie deleteRecoveryTokenCookie() {

        Cookie cookie = new Cookie("recoveryToken", "");

        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        return cookie;
    }
}