package com.school.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * {@code proxyTargetClass = true} est indispensable : sans lui, Spring génère un
 * proxy JDK pour les méthodes {@code @Async} des beans qui implémentent une
 * interface (comme {@code EmailService} / {@code ISendMail}), et l'injection
 * par type concret du bean échoue alors au démarrage. Le proxy CGLIB préserve la
 * classe réelle.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableAsync(proxyTargetClass = true)
@EnableMethodSecurity
public class SecurityApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurityApplication.class, args);
    }
}
