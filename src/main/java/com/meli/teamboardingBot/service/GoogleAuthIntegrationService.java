package com.meli.teamboardingBot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GoogleAuthIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthIntegrationService.class);
    
    @Value("${api.auth.google.url:https://api.test.tq.teamcubation.com/auth/google_login}")
    private String googleLoginUrl;
    
    private final RestTemplate restTemplate;

    public GoogleAuthIntegrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String authenticateWithInternalSystem(OAuth2User googleUser) {
        String googleEmail = googleUser.getAttribute("email");
        String googleName = googleUser.getAttribute("name");
        String nonce = googleUser.getAttribute("nonce");
        
        logger.info("Integrando usuário Google: {} ({})", googleName, googleEmail);
        logger.info("Authorization code (nonce): {}", nonce);
        
        if (nonce == null || nonce.isEmpty()) {
            logger.error("Nonce/code não encontrado nos atributos do Google OAuth2");
            throw new IllegalStateException("Authorization code não disponível");
        }
        
        try {
            String url = googleLoginUrl + "?code=" + nonce;
            
            logger.info("Chamando API interna: POST {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("accept", "application/json");
            
            HttpEntity<String> request = new HttpEntity<>("", headers);
            
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            
            if (response != null && response.containsKey("access_token")) {
                String accessToken = (String) response.get("access_token");
                logger.info("Token interno obtido com sucesso para usuário {}", googleEmail);
                logger.info("Token: {}...", accessToken.substring(0, Math.min(20, accessToken.length())));
                return accessToken;
            } else {
                logger.error("Resposta da API não contém access_token: {}", response);
                throw new RuntimeException("Token não encontrado na resposta");
            }
            
        } catch (Exception e) {
            logger.error("Erro ao autenticar usuário Google {} no sistema interno", googleEmail, e);
            throw new RuntimeException("Falha na autenticação interna: " + e.getMessage(), e);
        }
    }

    public String extractAuthorizationCode(OAuth2User googleUser) {
        return googleUser.getAttribute("nonce");
    }
}
