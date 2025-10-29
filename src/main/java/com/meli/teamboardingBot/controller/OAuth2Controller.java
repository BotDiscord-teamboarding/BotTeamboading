package com.meli.teamboardingBot.controller;

import com.meli.teamboardingBot.service.GoogleAuthIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class OAuth2Controller {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2Controller.class);
    private final GoogleAuthIntegrationService googleAuthIntegration;

    public OAuth2Controller(GoogleAuthIntegrationService googleAuthIntegration) {
        this.googleAuthIntegration = googleAuthIntegration;
    }

    @GetMapping("/oauth2/success")
    public Map<String, Object> oauth2Success(@AuthenticationPrincipal OAuth2User principal) {
        if (principal != null) {
            logger.info("=".repeat(80));
            logger.info("AUTENTICAÇÃO GOOGLE REALIZADA COM SUCESSO");
            logger.info("=".repeat(80));
            
            Map<String, Object> attributes = principal.getAttributes();
            
            logger.info("Nome: {}", attributes.get("name"));
            logger.info("Email: {}", attributes.get("email"));
            logger.info("ID: {}", attributes.get("sub"));
            logger.info("Foto: {}", attributes.get("picture"));
            logger.info("Email Verificado: {}", attributes.get("email_verified"));
            logger.info("Locale: {}", attributes.get("locale"));
            
            logger.info("-".repeat(80));
            logger.info("TODOS OS ATRIBUTOS:");
            attributes.forEach((key, value) -> 
                logger.info("  {} = {}", key, value)
            );
            logger.info("=".repeat(80));
            
            String internalToken = null;
            try {
                internalToken = googleAuthIntegration.authenticateWithInternalSystem(principal);
                logger.info("Token interno obtido com sucesso!");
            } catch (Exception e) {
                logger.warn("Não foi possível obter token interno: {}", e.getMessage());
            }
            
            return Map.of(
                "status", "success",
                "message", "Autenticação realizada com sucesso! Verifique o terminal para ver as credenciais.",
                "user", attributes,
                "internalTokenObtained", internalToken != null,
                "canAccessInternalApis", internalToken != null
            );
        }
        
        return Map.of(
            "status", "error",
            "message", "Usuário não autenticado"
        );
    }

    @GetMapping("/user")
    public Map<String, Object> user(@AuthenticationPrincipal OAuth2User principal) {
        if (principal != null) {
            return principal.getAttributes();
        }
        return Map.of("error", "Não autenticado");
    }
}
