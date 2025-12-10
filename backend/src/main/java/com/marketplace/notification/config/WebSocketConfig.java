package com.marketplace.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-notif")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Préfixe pour les messages que le backend envoie au frontend
        registry.enableSimpleBroker("/topic"); // ← CHANGER "/api" en "/topic"

        // Préfixe pour les messages que le frontend envoie au backend
        registry.setApplicationDestinationPrefixes("/app");
    }
}