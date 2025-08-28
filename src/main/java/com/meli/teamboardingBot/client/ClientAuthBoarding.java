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
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class ClientAuthBoarding {

    @Value("${api.url}")
    private String apiUrl;

    @Value("${api.username}")
    private String username;

    @Value("${api.password}")
    private String password;

    private String clientId = "string";
    private String clientSecret = "string";
    private final String authUrl = "/auth/login";

    Logger logger = LoggerFactory.getLogger(ClientAuthBoarding.class);
    private final RestTemplate restTemplate = new RestTemplate();

    public AuthTokenResponseDTO getToken() {
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "password");
        requestBody.add("username", username);
        requestBody.add("password", password);
        requestBody.add("scope", "");
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

        logger.info("Fazendo requisição de autenticação para: {}", apiUrl + authUrl);
        logger.debug("Request body: {}", requestBody);

        try {
            AuthTokenResponseDTO response = restTemplate.exchange(apiUrl + authUrl, HttpMethod.POST, request, AuthTokenResponseDTO.class).getBody();
            logger.info("Autenticação realizada com sucesso");
            return response;
        } catch (Exception e) {
            logger.error("Erro na autenticação: {}", e.getMessage());
            throw new RuntimeException("Falha na autenticação: " + e.getMessage(), e);
        }
    }


}
