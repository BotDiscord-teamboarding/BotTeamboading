package com.meli.teamboardingBot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleClientSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                            "/", 
                            "/error", 
                            "/webjars/**", 
                            "/oauth2/**", 
                            "/login/**"
                        ).permitAll()
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf.disable());

        if (isOAuth2Configured()) {
            logger.info("✅ Google OAuth2 CONFIGURADO. OAuth2 Login habilitado.");
            logger.info("   Endpoints disponíveis:");
            logger.info("   - Iniciar login: /oauth2/authorization/google");
            logger.info("   - Callback: /login/oauth2/code/google");
            logger.info("   - Success: /oauth2/success");
            
            http.oauth2Login(oauth2 -> oauth2
                    .defaultSuccessUrl("/oauth2/success", true)
            );
        } else {
            logger.warn("⚠️  Google OAuth2 NÃO CONFIGURADO");
            logger.warn("   Configure as variáveis de ambiente:");
            logger.warn("   - GOOGLE_CLIENT_ID");
            logger.warn("   - GOOGLE_CLIENT_SECRET");
            logger.warn("   Ou adicione no application.properties:");
            logger.warn("   - spring.security.oauth2.client.registration.google.client-id");
            logger.warn("   - spring.security.oauth2.client.registration.google.client-secret");
        }

        return http.build();
    }

    private boolean isOAuth2Configured() {
        return googleClientId != null && !googleClientId.trim().isEmpty() &&
               googleClientSecret != null && !googleClientSecret.trim().isEmpty();
    }
}
