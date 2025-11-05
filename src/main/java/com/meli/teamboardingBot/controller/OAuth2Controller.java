package com.meli.teamboardingBot.controller;

import com.meli.teamboardingBot.service.GoogleAuthIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@Controller
public class OAuth2Controller {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2Controller.class);
    private final GoogleAuthIntegrationService googleAuthIntegration;

    public OAuth2Controller(GoogleAuthIntegrationService googleAuthIntegration) {
        this.googleAuthIntegration = googleAuthIntegration;
    }

    @GetMapping("/oauth2/callback")
    public RedirectView oauth2Callback(@RequestParam(value = "code", required = false) String code,
                                  @RequestParam(value = "error", required = false) String error,
                                  @RequestParam(value = "error_description", required = false) String errorDescription,
                                  @RequestParam(value = "state", required = false) String state) {
        logger.info("=".repeat(80));
        logger.info("CALLBACK DO GOOGLE RECEBIDO");
        logger.info("Code: {}", code != null ? code.substring(0, Math.min(20, code.length())) + "..." : "null");
        logger.info("Error: {}", error);
        logger.info("Error Description: {}", errorDescription);
        logger.info("State: {}", state);
        logger.info("=".repeat(80));
        
        if (error != null) {
            logger.error("❌ Erro retornado pelo Google: {} - {}", error, errorDescription);
            return new RedirectView("/index.html");
        }
        
        if (code == null || code.trim().isEmpty()) {
            logger.error("❌ Code não fornecido no callback");
            return new RedirectView("/index.html");
        }
        
        try {
            String internalToken = googleAuthIntegration.authenticateWithGoogleCode(code);
            
            if (internalToken != null) {
                logger.info("✅ Token interno obtido com sucesso via Google code!");
            } else {
                logger.warn("⚠️  Falha ao obter token interno");
            }
        } catch (Exception e) {
            logger.error("❌ Erro ao processar callback do Google: {}", e.getMessage(), e);
        }
        
        return new RedirectView("/index.html");
    }

    @GetMapping("/oauth2/success")
    public RedirectView oauth2Success(@AuthenticationPrincipal OAuth2User principal) {
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
            
            try {
                String internalToken = googleAuthIntegration.authenticateWithInternalSystem(principal);
                logger.info("✅ Token interno obtido com sucesso!");
            } catch (Exception e) {
                logger.warn("⚠️  Não foi possível obter token interno: {}", e.getMessage());
            }
            
            return new RedirectView("/index.html");
        }
        
        logger.error("❌ Usuário não autenticado no callback");
        return new RedirectView("/index.html");
    }

    @GetMapping("/user")
    @ResponseBody
    public Map<String, Object> user(@AuthenticationPrincipal OAuth2User principal) {
        if (principal != null) {
            return principal.getAttributes();
        }
        return Map.of("error", "Não autenticado");
    }
}
