package com.sue.overlay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Dota 2 GSI шлёт POST без авторизации и CSRF-токена, оверлей читает API без логина —
 * поэтому по умолчанию (всё закрыто basic-auth) работать не будет.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }
}
