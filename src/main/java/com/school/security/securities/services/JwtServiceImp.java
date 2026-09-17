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

/**
 * Implémentation du service de génération et de validation des tokens JWT
 * (bibliothèque {@code io.jsonwebtoken} / jjwt).
 *
 * <p>Trois types de tokens produits, tous signés en HMAC-SHA (clé issue de
 * {@code jwt.secret.key}, configurée dans le code secret de l'application) :
 * <ul>
 *   <li>access token : 1 heure, sujet = email de l'utilisateur, claim
 *       {@code roles} = autorités (rôles) de l'utilisateur ;</li>
 *   <li>refresh token : 7 jours, mêmes informations, claim {@code roles}
 *       stocké en liste de chaînes ;</li>
 *   <li>recovery token : 10 minutes, claims {@code code} et
 *       {@code type = "RECOVERY"} (récupération de compte).</li>
 * </ul>
 *
 * <p>NOTE : les opérations de validation ({@code isTokenValid},
 * {@code extractUsername}, ...) analysent le token sans restreindre son type :
 * un refresh token ou un recovery token valide est aussi considéré "valide"
 * pour {@code isTokenValid} (du moment que le sujet correspond et que le
 * token n'est pas expiré). La séparation des usages repose donc sur le flux
 * des contrôleurs, pas sur le format du token.
 *
 * <p>NOTE : la méthode {@code getSiginKey()} contient une faute de frappe
 * ("Sigin" au lieu de "Signin") — conservée telle quelle pour ne pas casser
 * les appels existants.
 */
@Service
public class JwtServiceImp implements JwtService {
    @Value("${jwt.secret.key}")
    private String secretKey;

    /**
     * Génére l'access token (validité 1 heure).
     *
     * <p>Sujet = nom d'utilisateur (l'email ici). Le claim {@code roles}
     * contient les objets {@code GrantAuthority} tels que fournis par
     * {@code UserDetails} (attention : il n'est PAS converti en chaînes ici,
     * contrairement au refresh token).
     */
    @Override
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", userDetails.getAuthorities())
                .issuedAt(new Date(System.currentTimeMillis()))
                // Expiration à 1 heure (1000 * 60 * 60 ms).
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(getSiginKey())
                .compact();
    }

    /**
     * Extrait le sujet du token (= email de l'utilisateur).
     */
    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Applique une fonction d'extraction sur les claims du token.
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsTFunction) {
        final Claims claims = extractAllClaims(token);
        return claimsTFunction.apply(claims);
    }

    /**
     * Analyse un token JWT et retourne ses claims.
     *
     * <p>La clé de vérification est passée directement sous forme de chaîne
     * base64 ({@code secretKey}), alors que la génération utilise la clé
     * décodée (voir {@link #getSiginKey()}) — jjwt accepte les deux formats.
     * Un token invalide ou falsifié lève une exception (gérée par les
     * appelants).
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
    }

    /**
     * Construit la clé HMAC à partir du secret (base64) de l'application.
     *
     * <p>Le nom de méthode laisse apparaître une faute de frappe d'origine
     * ("getSiginKey") — non corrigée pour rester compatible.
     */
    private Key getSiginKey() {
        byte[] key = Decoders.BASE64.decode(this.secretKey);
        return Keys.hmacShaKeyFor(key);
    }

    /**
     * Vérifie qu'un token est valide pour un utilisateur donné : le sujet du
     * token doit correspondre au nom d'utilisateur ET le token ne doit pas
     * être expiré.
     *
     * <p>Aucune distinction de type de token n'est faite ici (voir la note
     * de classe).
     */
    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Génére le refresh token (validité 7 jours).
     *
     * <p>Le claim {@code roles} est ici converti en liste de chaînes
     * (noms des autorités), contrairement au {@link #generateToken}.
     */
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
                // Expiration à 7 jours (604 800 000 ms).
                .expiration(new Date(System.currentTimeMillis() + 604800000))
                .signWith(getSiginKey())
                .compact();
    }

    /**
     * Indique si le token est expiré (date d'expiration antérieure à
     * l'instant courant).
     */
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Génére un token de récupération de compte lié à un code à usage unique :
     * claims {@code code} (4 chiffres) et {@code type = "RECOVERY"}, sujet =
     * email de l'utilisateur, validité 10 minutes.
     */
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

    /**
     * Extrait le code de récupération (claim {@code code}) d'un token.
     */
    @Override
    public int extractRecoveryCode(String token) {
        return extractAllClaims(token)
                .get("code", Integer.class);
    }

    /**
     * Vérifie qu'un token est un token de récupération valide : le claim
     * {@code type} doit valoir {@code "RECOVERY"} et le token ne doit pas
     * être expiré. Retourne {@code false} sur toute exception (token
     * invalide, falsifié ou expiré).
     */
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
