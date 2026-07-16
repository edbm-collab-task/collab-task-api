package com.school.security.controllers.auth;


import com.school.security.dtos.requests.LoginReqDto;
import com.school.security.dtos.requests.UserReqDto;
import com.school.security.dtos.responses.CodeResDto;
import com.school.security.dtos.responses.UserResDto;
import com.school.security.mappers.UserMapper;
import com.school.security.securities.services.JwtService;
import com.school.security.services.contracts.UserService;


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
@CrossOrigin(
        origins = {
                "http://localhost:3000",
                "http://192.168.1.133:3000"
        },
        allowCredentials = "true"
)
public class AuthController {


    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;



    public AuthController(
            UserService userService,
            AuthenticationManager authenticationManager,
            JwtService jwtService, UserMapper userMapper
    ) {

        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }





    /**
     * LOGIN
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReqDto credential, HttpServletResponse response) {

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(credential.email(), credential.password()));
        var user = userService.findByEmail(credential.email());

        // Utilisateur connecté
        user.setStatus(true);
        userService.createOrUpdate(userMapper.toUserReq(user));

        var jwt = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(new HashMap<>(), user);

        Cookie accessCookie = new Cookie("accessToken", jwt);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(15 * 60);

        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60);

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(Map.of("roles", user.getRoles().getFirst().getName()));
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
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = getCookie(request, "refreshToken");

        if(refreshToken == null){
            return ResponseEntity.badRequest().body(Map.of("message", "Refresh token missing"));
        }

        String username = jwtService.extractUsername(refreshToken);

        var user = userService.findByEmail(username);

        if(jwtService.isTokenValid(refreshToken, user)){

            var newAccessToken = jwtService.generateToken(user);
            Cookie accessCookie = new Cookie("accessToken", newAccessToken);

            accessCookie.setHttpOnly(true);
            accessCookie.setSecure(false);
            accessCookie.setPath("/");
            accessCookie.setMaxAge(15 * 60);

            response.addCookie(accessCookie);

            return ResponseEntity.ok(Map.of("roles", user.getRoles().getFirst().getName()));
        }

        return ResponseEntity.status(401).body(Map.of("message", "Invalid refresh token"));
    }

    /**
     * GENERATE CODE
     */
    @PostMapping("/code")
    public CodeResDto generateCode(@RequestBody String email) {

        var user = userService.findByEmail(email);

        if(user == null)
        {
            throw new RuntimeException("User not defined");
        }

        var jwt = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(new HashMap<>(), user);

        int code = (int)(Math.random() * 900000) + 100000;

        System.out.println("Code : " + code);

        return new CodeResDto(code, jwt, refreshToken);
    }

    /**
     * LOGOUT
     */
    /**
     * LOGOUT
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {

        String token = getCookie(request, "accessToken");

        if(token != null){

            String email = jwtService.extractUsername(token);

            var user = userService.findByEmail(email);

            if(user != null){
                user.setStatus(false);
                userService.createOrUpdate(userMapper.toUserReq(user));
            }
        }


        Cookie accessCookie = new Cookie("accessToken", null);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);


        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);


        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);


        return ResponseEntity.ok(
                Map.of("message", "Logout successful")
        );
    }

    /**
     * Lire un cookie
     */
    private String getCookie(HttpServletRequest request, String name) {

        Cookie[] cookies = request.getCookies();

        if(cookies != null){
            for(Cookie cookie : cookies){
                if(cookie.getName()
                        .equals(name)){
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

}