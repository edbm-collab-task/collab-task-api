package com.school.security.controllers.auth;


import com.school.security.core.email.EmailService;
import com.school.security.dtos.requests.EmailReq;
import com.school.security.dtos.requests.LoginReqDto;
import com.school.security.dtos.requests.UserReqDto;
import com.school.security.dtos.responses.LoginResDto;
import com.school.security.dtos.responses.UserResDto;
import com.school.security.mappers.UserMapper;
import com.school.security.securities.services.JwtService;
import com.school.security.securities.utils.CookieUtils;
import com.school.security.services.contracts.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;



@RestController
@Slf4j
@RequestMapping("/auth")

public class AuthController {


    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService;



    public AuthController(
            UserService userService,
            AuthenticationManager authenticationManager,
            JwtService jwtService, UserMapper userMapper, EmailService emailService
    ) {

        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    /**
     * LOGIN
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResDto> login(
            @RequestBody LoginReqDto credential,
            HttpServletResponse response
    ) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        credential.email(),
                        credential.password()
                )
        );

        var user = userService.findByEmail(credential.email());

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(new HashMap<>(), user);

        response.addCookie(
                CookieUtils.createAccessTokenCookie(accessToken)
        );

        response.addCookie(
                CookieUtils.createRefreshTokenCookie(refreshToken)
        );

        return ResponseEntity.ok(
                new LoginResDto(
                        user.getRoles().getFirst().getName(),
                        user.getFirstname(),
                        user.getLastname(),
                        user.getEmail()
                )
        );
    }

    /**
     * REGISTER
     */
    @PostMapping("/register")
    public UserResDto register(@RequestBody UserReqDto userReqDto)
    {
        return userService.createOrUpdate(userReqDto);
    }

    /**
     * REFRESH TOKEN
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        String refreshToken = getCookie(request, "refreshToken");

        if (refreshToken == null || refreshToken.isBlank()) {

            return ResponseEntity.status(401).body(
                    Map.of(
                            "message",
                            "Refresh token missing"
                    )
            );
        }

        try {

            String email = jwtService.extractUsername(refreshToken);

            var user = userService.findByEmail(email);

            if (user == null) {

                return ResponseEntity.status(401).body(
                        Map.of(
                                "message",
                                "User not found"
                        )
                );
            }

            if (!jwtService.isTokenValid(refreshToken, user)) {

                return ResponseEntity.status(401).body(
                        Map.of(
                                "message",
                                "Invalid refresh token"
                        )
                );
            }

            /*
             * Rotation du Refresh Token
             */

            String newAccessToken = jwtService.generateToken(user);

            String newRefreshToken =
                    jwtService.generateRefreshToken(new HashMap<>(), user);

            response.addCookie(
                    CookieUtils.createAccessTokenCookie(newAccessToken)
            );

            response.addCookie(
                    CookieUtils.createRefreshTokenCookie(newRefreshToken)
            );

            return ResponseEntity.ok(
                    new LoginResDto(
                            user.getRoles().getFirst().getName(),
                            user.getFirstname(),
                            user.getLastname(),
                            user.getEmail()
                    )
            );

        } catch (Exception e) {

            response.addCookie(CookieUtils.deleteAccessTokenCookie());
            response.addCookie(CookieUtils.deleteRefreshTokenCookie());

            return ResponseEntity.status(401).body(
                    Map.of(
                            "message",
                            "Refresh token expired"
                    )
            );
        }
    }

    /**
     * GENERATE CODE
     */
    @PostMapping("/code")
    public ResponseEntity<?> generateCode(
            @RequestBody EmailReq emailReq,
            HttpServletResponse response
    ) {

        var user = userService.findByEmail(emailReq.email());

        int code = (int) (Math.random() * 900000) + 100000;

        String recoveryToken =
                jwtService.generateRecoveryToken(user, code);

        String email = user.getEmail();

        response.addCookie(
                CookieUtils.createRecoveryTokenCookie(recoveryToken)

        );

        response.addCookie(
                CookieUtils.createEmail(email)
        );

        emailService.sendRecoveryCodeEmail(
                user.getEmail(),
                user.getFirstname(),
                code
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Le code de récupération a été envoyé à votre adresse email."
                )
        );
    }

    @GetMapping("/verification-code")
    public ResponseEntity<?> verification(
            @RequestParam int code,
            HttpServletRequest request
    ) {

        String recoveryToken = getCookie(request, "recoveryToken");

        if (recoveryToken == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Token introuvable"));
        }

        try {

            int tokenCode = jwtService.extractRecoveryCode(recoveryToken);

            if (tokenCode != code) {

                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Code incorrect"));
            }

            return ResponseEntity.ok(
                    Map.of(
                            "message", "Code valide",
                            "valid", true
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.status(401)
                    .body(Map.of("message", "Code expiré"));
        }
    }

    /**
     * LOGOUT
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {

        response.addCookie(
                CookieUtils.deleteAccessTokenCookie()
        );

        response.addCookie(
                CookieUtils.deleteRefreshTokenCookie()
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Logout successful"
                )
        );
    }

    /**
     * Lire un cookie
     */
    private String getCookie(HttpServletRequest request, String name) {

        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {

            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }

        return null;
    }

    @PutMapping("/status")
    public ResponseEntity<?> updateStatus(
            @RequestParam String email,
            @RequestParam Boolean status
    ) {
        userService.updateStatus(email, status);

        return ResponseEntity.ok(
                Map.of("message", "Status updated successfully")
        );
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResDto> me(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();

        var user = userService.findByEmail(email);

        return ResponseEntity.ok(
                new LoginResDto(
                        user.getRoles().getFirst().getName(),
                        user.getFirstname(),
                        user.getLastname(),
                        user.getEmail()
                )
        );
    }

    @GetMapping("/recovery/me")
    public ResponseEntity<?> recoveryMe(
            @CookieValue(value = "recoveryToken", required = false) String recoveryToken
    ) {

        if (recoveryToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Recovery token missing"));
        }

        if (!jwtService.isRecoveryTokenValid(recoveryToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Recovery token invalid"));
        }

        String email = jwtService.extractUsername(recoveryToken);

        return ResponseEntity.ok(
                Map.of(
                        "email", email,
                        "recovery", true
                )
        );
    }

}