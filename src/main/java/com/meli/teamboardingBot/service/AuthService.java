package com.meli.teamboardingBot.service;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    @Value("${api.auth.url}")
    private String authUrl;

    @Value("${api.auth.username}")
    private String username;

    @Value("${api.auth.password}")
    private String password;

    Logger logger = LoggerFactory.getLogger(AuthService.class);

    public String getToken() {
        ResponseEntity<Map> response = generateToken();

        logger.debug("Response Body: " + response.getBody());

        if (response.getBody() != null && response.getBody().containsKey("access_token")) {
            return response.getBody().get("access_token").toString();
        }

        throw new RuntimeException("Token não encontrado na resposta");
    }

    public int getUserClientId() {
        ResponseEntity<Map> tokenResponse = generateToken();
        Map<String, Object> body = tokenResponse.getBody();

        if (body == null || !body.containsKey("user")) {
            throw new RuntimeException("Usuário não encontrado na resposta");
        }

        Map<String, Object> user = (Map<String, Object>) body.get("user");
        if (user == null || !user.containsKey("clients")) {
            throw new RuntimeException("Clients não encontrados na resposta do usuário");
        }

        Object clientsObj = user.get("clients");
        if (clientsObj instanceof List<?> clientsList && !clientsList.isEmpty()) {
            Object firstClient = clientsList.get(0);
            if (firstClient instanceof Map<?, ?> clientMap && clientMap.containsKey("id")) {
                Object idObj = clientMap.get("id");
                if (idObj instanceof Integer) {
                    return (Integer) idObj;
                } else if (idObj instanceof String) {
                    return Integer.parseInt((String) idObj);
                }
            }
        }

        throw new RuntimeException("client_id não encontrado na resposta");
    }


    @NotNull
    private ResponseEntity<Map> generateToken() {
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "password");
        requestBody.add("username", username);
        requestBody.add("password", password);
        requestBody.add("scope", "");
        requestBody.add("client_id", "string");
        requestBody.add("client_secret", "string");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

        return restTemplate.exchange(authUrl, HttpMethod.POST, request, Map.class);
    }
}
