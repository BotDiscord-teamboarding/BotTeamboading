package com.meli.teamboardingBot.service;

import com.meli.teamboardingBot.client.ClientAuthBoarding;
import com.meli.teamboardingBot.dto.AuthTokenResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class GoogleAuthIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthIntegrationService.class);
    private final ClientAuthBoarding authClient;
    private final RestTemplate restTemplate;

    @Value("${api.url}")
    private String apiUrl;

    @Value("${api.auth.google.connection.url}")
    private String googleAuthApiUrl;

    private static final String GOOGLE_AUTH_URL = "https://darian-dainties-terese.ngrok-free.dev/oauth2/authorization/google";


    public GoogleAuthIntegrationService(ClientAuthBoarding authClient, RestTemplate restTemplate) {
        this.authClient = authClient;
        this.restTemplate = restTemplate;
    }

    public String getGoogleAuthUrl() {
        logger.info("Retornando URL de autenticação Google: {}", GOOGLE_AUTH_URL);
        return GOOGLE_AUTH_URL;
    }

    public String authenticateWithGoogleCode(String code) throws Exception {
        logger.info("🔐 Iniciando autenticação com Google code");
        logger.info("   Code recebido: {}...", code.substring(0, Math.min(20, code.length())));
        
        try {
            String googleLoginUrl = apiUrl + "/auth/google_login?code=" + code;
            logger.info("📡 Chamando endpoint: {}", googleLoginUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            
            HttpEntity<String> request = new HttpEntity<>("", headers);
            
            ResponseEntity<AuthTokenResponseDTO> response = restTemplate.exchange(
                googleLoginUrl,
                HttpMethod.POST,
                request,
                AuthTokenResponseDTO.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                AuthTokenResponseDTO tokenResponse = response.getBody();
                if (tokenResponse.getAccessToken() != null) {
                    logger.info("✅ Token obtido com sucesso da API interna!");
                    
                    if (tokenResponse.getUser() != null && tokenResponse.getUser().getEmail() != null) {
                        String email = tokenResponse.getUser().getEmail();
                        logger.info("   Email do usuário: {}", email);
                        authClient.setCredentials(email, code);
                    }
                    
                    return tokenResponse.getAccessToken();
                }
            }
            
            logger.warn("⚠️  Resposta da API não contém token válido");
            return null;
            
        } catch (Exception e) {
            logger.error("❌ Erro ao autenticar com Google code: {}", e.getMessage(), e);
            throw new Exception("Falha ao autenticar com Google: " + e.getMessage());
        }
    }

    public String authenticateWithInternalSystem(OAuth2User oauth2User) throws Exception {
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String googleId = oauth2User.getAttribute("sub");
        String picture = oauth2User.getAttribute("picture");
        
        logger.info("🔐 Iniciando autenticação automática via Google OAuth");
        logger.info("   Email: {}", email);
        logger.info("   Nome: {}", name);
        logger.info("   Google ID: {}", googleId);
        
        String currentUsername = authClient.getUsername();
        if (currentUsername != null && currentUsername.equals(email)) {
            try {
                AuthTokenResponseDTO tokenResponse = authClient.getToken();
                if (tokenResponse != null && tokenResponse.getAccessToken() != null) {
                    logger.info("✅ Token em cache encontrado para: {}", email);
                    return tokenResponse.getAccessToken();
                }
            } catch (IllegalStateException e) {
                logger.debug("Token em cache não disponível, tentando outras estratégias...");
            }
        }
        
        try {
            logger.info("🔄 Tentando autenticação via endpoint Google da API interna...");
            String token = authenticateViaGoogleEndpoint(email, googleId, name, picture);
            if (token != null) {
                logger.info("✅ Autenticação via Google endpoint bem-sucedida!");
                authClient.setCredentials(email, googleId);
                return token;
            }
        } catch (Exception e) {
            logger.warn("⚠️  Autenticação via Google endpoint falhou: {}", e.getMessage());
        }
        

        try {
            logger.info("🔄 Tentando autenticação com email do Google no sistema interno...");
            AuthTokenResponseDTO tokenResponse = authClient.getToken(email, googleId);
            if (tokenResponse != null && tokenResponse.getAccessToken() != null) {
                logger.info("✅ Autenticação com email do Google bem-sucedida!");
                authClient.setCredentials(email, googleId);
                return tokenResponse.getAccessToken();
            }
        } catch (Exception e) {
            logger.warn("⚠️  Autenticação com email do Google falhou: {}", e.getMessage());
        }
        
        logger.error("❌ Todas as estratégias de autenticação falharam para: {}", email);
        throw new Exception(
            "Não foi possível autenticar automaticamente com o sistema interno. "
        );
    }
    

    private String authenticateViaGoogleEndpoint(String email, String googleId, String name, String picture) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("email", email);
            requestBody.put("google_id", googleId);
            requestBody.put("name", name);
            requestBody.put("picture", picture);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            logger.info("Enviando requisição para: {}", googleAuthApiUrl);
            
            ResponseEntity<AuthTokenResponseDTO> response = restTemplate.exchange(
                googleAuthApiUrl,
                HttpMethod.POST,
                request,
                AuthTokenResponseDTO.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                AuthTokenResponseDTO tokenResponse = response.getBody();
                if (tokenResponse.getAccessToken() != null) {
                    return tokenResponse.getAccessToken();
                }
            }
            
            return null;
            
        } catch (Exception e) {
            logger.debug("Erro ao tentar endpoint Google: {}", e.getMessage());
            return null;
        }
    }
}