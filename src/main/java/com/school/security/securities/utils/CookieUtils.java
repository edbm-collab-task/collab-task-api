package com.school.security.securities.utils;
import jakarta.servlet.http.Cookie;

/**
 * Fabrique statique des cookies HTTP utilisés pour l'authentification JWT
 * et la récupération de compte.
 *
 * <p>Tous les cookies créés ici sont en {@code HttpOnly} (non lisibles par
 * JavaScript), avec un chemin {@code "/"} et le flag {@code Secure} désactivé
 * (les commentaires en ligne indiquent l'intention de l'activer en
 * production HTTPS).
 *
 * <p>Cookies gérés :
 * <ul>
 *   <li>{@code email} : mémorise l'email pour pré-remplissage côté front ;</li>
 *   <li>{@code accessToken} : access token JWT (durée de vie du cookie
 *       15 minutes) ;</li>
 *   <li>{@code refreshToken} : refresh token JWT (7 jours) ;</li>
 *   <li>{@code recoveryToken} : token de récupération de compte (10 minutes).</li>
 * </ul>
 *
 * <p>Les méthodes {@code deleteXxx} renvoient un cookie de même nom, à valeur
 * vide et {@code maxAge = 0} : l'objectif est d'expirer le cookie chez le
 * client. Exception : {@link #deleteEmail()} conserve {@code maxAge = 15
 * minutes}, il ne fait donc qu'écraser la valeur sans expiration.
 */
public class CookieUtils {

    private CookieUtils() {
    }

    /**
     * Crée le cookie {@code email} (valeur l'email de l'utilisateur),
     * prévu pour être conservé 15 minutes.
     */
    public static Cookie createEmail(String email) {

        Cookie cookie = new Cookie("email", email);

        cookie.setHttpOnly(true);
        cookie.setSecure(false); // true en production HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(15 * 60);

        return cookie;
    }

    /**
     * Crée un cookie {@code email} vide pour "effacer" l'email.
     *
     * <p>NOTE : contrairement aux autres méthodes {@code deleteXxx}, ce cookie
     * ne fixe pas {@code maxAge = 0} mais le conserve à 15 minutes : la valeur
     * est vidée mais le cookie ne sera pas expiré immédiatement côté client.
     */
    public static  Cookie deleteEmail(){
        Cookie cookie = new Cookie("email", "");

        cookie.setHttpOnly(true);
        cookie.setSecure(false); // true en production HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(15 * 60);

        return cookie;
    }

    /**
     * Crée le cookie {@code accessToken} contenant l'access token JWT.
     *
     * <p>Durée de vie du cookie : 15 minutes (à ne pas confondre avec la
     * durée de vie du token JWT lui-même, fixée à 1 heure — voir
     * {@code JwtServiceImp.generateToken}).
     */
    public static Cookie createAccessTokenCookie(String token) {

        Cookie cookie = new Cookie("accessToken", token);

        cookie.setHttpOnly(true);
        cookie.setSecure(false); // true en production HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(15 * 60);

        return cookie;
    }

    /**
     * Crée le cookie {@code refreshToken} contenant le refresh token JWT.
     *
     * <p>Durée de vie du cookie : 7 jours (alignée sur la durée de vie du
     * refresh token lui-même — voir
     * {@code JwtServiceImp.generateRefreshToken}).
     */
    public static Cookie createRefreshTokenCookie(String token) {

        Cookie cookie = new Cookie("refreshToken", token);

        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);

        return cookie;
    }

    /**
     * Crée un cookie {@code accessToken} vide avec {@code maxAge = 0} :
     * expire l'access token chez le client.
     */
    public static Cookie deleteAccessTokenCookie() {

        Cookie cookie = new Cookie("accessToken", "");

        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        return cookie;
    }

    /**
     * Crée un cookie {@code refreshToken} vide avec {@code maxAge = 0} :
     * expire le refresh token chez le client.
     */
    public static Cookie deleteRefreshTokenCookie() {

        Cookie cookie = new Cookie("refreshToken", "");

        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        return cookie;
    }

    /**
     * Crée le cookie {@code recoveryToken} contenant le token de récupération
     * de compte.
     *
     * <p>Durée de vie du cookie : 10 minutes (alignée sur la durée de vie du
     * token de récupération — voir {@code JwtServiceImp.generateRecoveryToken}).
     */
    public static Cookie createRecoveryTokenCookie(String token) {

        Cookie cookie = new Cookie("recoveryToken", token);

        cookie.setHttpOnly(true);
        cookie.setSecure(false); // true en production HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(10 * 60); // 10 minutes

        return cookie;
    }

    /**
     * Crée un cookie {@code recoveryToken} vide avec {@code maxAge = 0} :
     * expire le token de récupération chez le client.
     */
    public static Cookie deleteRecoveryTokenCookie() {

        Cookie cookie = new Cookie("recoveryToken", "");

        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        return cookie;
    }
}