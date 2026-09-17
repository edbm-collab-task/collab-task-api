package com.school.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Configuration MVC : exposition statique des fichiers téléversés.
 *
 * <p>Fonctionnement constaté (documenté, non modifié) :
 * <ul>
 *   <li>les requêtes {@code /api/uploads/**} sont servies depuis le répertoire
 *       défini par la propriété {@code app.upload.dir} (défaut {@code uploads},
 *       relatif au répertoire de travail), résolu en chemin absolu normalisé ;</li>
 *   <li>{@code FileStorageService} écrit bien sous ce même répertoire, mais les
 *       URL qu'il retourne utilisent un préfixe littéral {@code "uploads/..."} ;
 *       le mapping n'est donc cohérent que si {@code app.upload.dir} vaut
 *       {@code uploads}.</li>
 * </ul>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Répertoire racine des uploads ({@code app.upload.dir}, défaut {@code uploads}). */
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /** Expose le contenu du répertoire d'uploads sous {@code /api/uploads/**}. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        String absolutePath = Paths
                .get(uploadDir)
                .toAbsolutePath()
                .normalize()
                .toString();

        registry.addResourceHandler("/api/uploads/**")
                .addResourceLocations(
                        "file:" + absolutePath + "/"
                );
    }
}