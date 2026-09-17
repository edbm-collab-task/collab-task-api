package com.school.security.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration STOMP/WebSocket de l'application.
 *
 * <p>Réglages constatés (documentés, non modifiés) :
 * <ul>
 *   <li>broker simple activé sur le préfixe {@code /topic} : les clients
 *       s'abonnent aux destinations sous {@code /topic/...} ;</li>
 *   <li>préfixe des destinations applicatives {@code /app} : les messages
 *       envoyés par les clients vers {@code /app/...} sont routés vers les
 *       méthodes {@code @MessageMapping} ;</li>
 *   <li>endpoint de handshake exposé sur {@code /ws} ;</li>
 *   <li>seule origine autorisée : {@code http://localhost:5173} (frontend de
 *       développement). Aucun endpoint SockJS n'est enregistré
 *       ({@code withSockJS()} absent).</li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig
        implements WebSocketMessageBrokerConfigurer {

    /**
     * Déclare le broker simple {@code /topic} et le préfixe applicatif
     * {@code /app}.
     */
    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry registry
    ) {

        registry.enableSimpleBroker(
                "/topic"
        );

        registry.setApplicationDestinationPrefixes(
                "/app"
        );
    }

    /**
     * Enregistre l'endpoint de handshake {@code /ws} et restreint les origines
     * du navigateur à {@code http://localhost:5173}.
     */
    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry
    ) {

        registry.addEndpoint(
                        "/ws"
                )
                .setAllowedOriginPatterns(
                        "http://localhost:5173"
                );
    }
}