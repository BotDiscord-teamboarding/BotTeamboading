package com.meli.teamboardingBot.client;

import com.meli.teamboardingBot.dto.AuthTokenResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class ClientBoarding {

    @Value("${api.url}")
    private String apiUrl;

    @Value("${api.username}")
    private String username;

    @Value("${api.password}")
    private String password;

    @Autowired
    private ClientAuthBoarding authBoarding;

    private final Logger logger = LoggerFactory.getLogger(ClientBoarding.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final String SQUAD_LIST_PATH = "/clients/squads?only_active_squads=true&limit=50&offset=0";
    private final String SQUAD_LOG_PATH = "/clients/squad_logs";
    private final String SQUAD_LOGTYPE_PATH = "/clients/squad_log_types/all";
    private final String SQUAD_CATEGORY_PATH = "/clients/skill_categories";
    private final String SQUAD_LOG_LIST_ALL_PATH = "/clients/squad_logs?offset=0&q=&client_id=67&area_id=67&project_id=35&squad_id=232&only_active_squads=true&limit=15";
    private AuthTokenResponseDTO getAuthToken() {
        return authBoarding.getToken();
    }

    private Long getClientId() {
        return getAuthToken().getUser().getId();
    }


    public String getSquads() {
        AuthTokenResponseDTO token = getAuthToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.getAccessToken());
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        String fullUrl = apiUrl + SQUAD_LIST_PATH;
        logger.info("Fazendo requisição para buscar squads: {}", fullUrl);
        
        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl, HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    public String getSquadLogTypes() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAuthToken().getAccessToken());
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl + SQUAD_LOGTYPE_PATH, HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    public String getSquadCategories() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAuthToken().getAccessToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = apiUrl + SQUAD_CATEGORY_PATH + "?client_id=" + getClientId();
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, request, String.class);
        return response.getBody();
    }


    public ResponseEntity<String> createSquadLog(String payload) {
        AuthTokenResponseDTO token = getAuthToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<String> request = new HttpEntity<>(payload, headers);
        
        String fullUrl = apiUrl + SQUAD_LOG_PATH;
        logger.info("Fazendo requisição para criar squad log: {}", fullUrl);
        logger.debug("Payload: {}", payload);
        
        return restTemplate.exchange(fullUrl, HttpMethod.POST, request, String.class);
    }

    public String getSquadLogAll() {
        AuthTokenResponseDTO token = getAuthToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.getAccessToken());
        HttpEntity<Void> request = new HttpEntity<>(headers);

        String fullUrl = apiUrl + SQUAD_LOG_LIST_ALL_PATH;
        logger.info("Fazendo requisição para buscar todas as squads log: {}", fullUrl);

        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl, HttpMethod.GET, request, String.class);
        return response.getBody();
    }
}
