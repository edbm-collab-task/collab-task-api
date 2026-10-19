package com.school.security.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration globale de la documentation OpenAPI (Swagger UI).
 *
 * <p>Définit les métadonnées générales de l'API et le schéma de sécurité JWT
 * afin que le bouton « Authorize » de Swagger UI permette de saisir un
 * {@code accessToken} et de tester les endpoints protégés.
 *
 * <p>Les descriptions par contrôleur et par endpoint sont portées par les
 * annotations {@code @Tag} et {@code @Operation} directement dans les
 * contrôleurs (en français, pour faciliter la reprise du code).
 */
@Configuration
@OpenAPIDefinition(
        info =
                @Info(
                        title = "CollabTask API",
                        version = "1.0",
                        description =
                                "API de gestion collaborative de tâches : utilisateurs, rôles et "
                                        + "permissions (masques d'actions), projets, tâches, "
                                        + "commentaires, pièces jointes, messagerie, notifications "
                                        + "et rapports."))
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI collabTaskOpenAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        SECURITY_SCHEME_NAME,
                                        new SecurityScheme()
                                                .name(SECURITY_SCHEME_NAME)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description(
                                                        "Jeton JWT (accessToken) à placer dans "
                                                                + "l'en-tête Authorization: Bearer <token>.")));
    }
}
