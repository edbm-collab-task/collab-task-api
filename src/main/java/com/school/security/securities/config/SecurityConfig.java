package com.school.security.securities.config;

import com.school.security.services.contracts.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/ws").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login","/auth/create", "/auth/register", "/auth/logout", "/auth/refresh", "/auth/code").permitAll()
                        .requestMatchers(HttpMethod.POST,"/users/{id}/image").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users", "/users/*", "/users/email","/users/active","/users/disable").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/{id}/image").permitAll()
                        .requestMatchers(HttpMethod.PUT,"/users/pwd","/users/role").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/users/account").hasAnyAuthority("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT,"/auth/status").permitAll()
                        .requestMatchers(HttpMethod.GET,"/auth/me","/auth/verification-code","/auth/recovery/me").permitAll()
                        .requestMatchers(HttpMethod.GET, "/directions", "/directions/{id}").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/directions/{id}").permitAll()
                        .requestMatchers(HttpMethod.POST, "/directions").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/directions/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/messages/{filename}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/roles", "/roles/permissions").permitAll()
                        .requestMatchers(HttpMethod.POST, "/roles").hasAuthority("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/roles/{id}").hasAuthority("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/roles/{id}").hasAuthority("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/users/admins").hasAuthority("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/dashboard/reports/pdf").hasAnyAuthority("ADMIN", "SUPER_ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService.userDetailsService());
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Set-Cookie"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}