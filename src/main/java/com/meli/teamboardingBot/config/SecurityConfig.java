package com.meli.teamboardingBot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.info("=".repeat(80));
        logger.info("CONFIGURANDO SPRING SECURITY - SecurityFilterChain (SEM OAuth2)");
        logger.info("=".repeat(80));

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> {
                    logger.info("Configurando regras de autorização:");
                    logger.info("  - Permitindo acesso a todos os endpoints");

                    authorize.anyRequest().permitAll();
                });

        logger.info("Spring Security configurado com sucesso!");
        logger.info("=".repeat(80));

        return http.build();
    }
}