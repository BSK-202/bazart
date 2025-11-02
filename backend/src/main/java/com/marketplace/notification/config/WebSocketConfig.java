package com.marketplace.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Le frontend se connectera ici
        registry.addEndpoint("/ws-notif").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Préfixe pour destinations (backend -> frontend)
        registry.enableSimpleBroker("/topic");
        // Préfixe pour les messages envoyés par le frontend au backend
        registry.setApplicationDestinationPrefixes("/app");
    }
}