package com.sue.overlay.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Префикс для отправки сообщений клиентам (оверлею)
        config.enableSimpleBroker("/topic");
        // Префикс для сообщений от клиентов (нам не нужно, но пусть будет)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Точка подключения для оверлея. SockJS нужен для совместимости с OBS
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}