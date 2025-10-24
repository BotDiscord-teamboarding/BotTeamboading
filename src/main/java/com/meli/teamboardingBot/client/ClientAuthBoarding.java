package com.meli.teamboardingBot.client;

import com.meli.teamboardingBot.dto.AuthTokenResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.time.Duration;
import java.util.List;

@Component
public class ClientAuthBoarding {
    @Value("${api.url}")
    private String apiUrl;
    private String username;
    private String password;
    @Value("${api.client.id:}")
    private String clientId;
    @Value("${api.client.secret:}")
    private String clientSecret;
    
    private final String authUrl = "/auth/login";
    private final Logger logger = LoggerFactory.getLogger(ClientAuthBoarding.class);
    private final RestTemplate restTemplate;
    private AuthTokenResponseDTO cachedToken;
    private long tokenExpirationTime;
    
    public ClientAuthBoarding() {
        this.restTemplate = new RestTemplateBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(15))
            .build();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setCredentials(String username, String password) {
        logger.info("Definindo novas credenciais para o usuário: {}", username);
        this.username = username;
        this.password = password;
        this.cachedToken = null;
        this.tokenExpirationTime = 0; // Força renovação do token
    }

    public AuthTokenResponseDTO getToken() {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpirationTime) {
            logger.debug("Retornando token em cache");
            return cachedToken;
        }

        if (username == null || password == null) {
            throw new IllegalStateException("Credenciais não configuradas. Por favor, faça login primeiro.");
        }

        return getToken(username, password);
    }


    public AuthTokenResponseDTO getToken(String username, String password) {
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "password");
        requestBody.add("username", username);
        requestBody.add("password", password);
        requestBody.add("scope", "");
        if (clientId != null && !clientId.trim().isEmpty()) {
            requestBody.add("client_id", clientId);
        }
        if (clientSecret != null && !clientSecret.trim().isEmpty()) {
            requestBody.add("client_secret", clientSecret);
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);
        logger.info("Fazendo requisição de autenticação para: {}", apiUrl + authUrl);
        
        long startTime = System.currentTimeMillis();
        try {
            AuthTokenResponseDTO response = restTemplate.exchange(
                apiUrl + authUrl, 
                HttpMethod.POST, 
                request, 
                AuthTokenResponseDTO.class
            ).getBody();
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Autenticação realizada com sucesso em {}ms", duration);
            
            if (username.equals(this.username) && password.equals(this.password)) {
                this.cachedToken = response;
                this.tokenExpirationTime = System.currentTimeMillis() + (3600 * 1000L);
            }
            
            return response;
            
        } catch (ResourceAccessException e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Timeout na autenticação após {}ms: {}", duration, e.getMessage());
            throw new RuntimeException("Timeout na conexão com a API (" + duration + "ms): " + e.getMessage(), e);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Erro na autenticação após {}ms: {}", duration, e.getMessage());
            throw new RuntimeException("Falha na autenticação: " + e.getMessage(), e);
        }
    }
}
