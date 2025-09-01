package com.meli.teamboardingBot.client;

import com.meli.teamboardingBot.dto.AuthTokenResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final String SQUAD_LOG_ID_PATH = "/clients/squad_logs/";
    private AuthTokenResponseDTO getAuthToken() {
        return authBoarding.getToken();
    }

    private Long getClientId() {
        return getAuthToken().getUser().getId();
    }

    public String getSquads() {
        AuthTokenResponseDTO token = getAuthToken();
        logger.info("Token obtido: {}", token.getAccessToken() != null ? "Token presente" : "Token nulo");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token.getAccessToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> request = new HttpEntity<>(headers);

        String fullUrl = apiUrl + SQUAD_LIST_PATH;
        logger.info("Fazendo requisição para buscar squads: {}", fullUrl);
        logger.info("Authorization header: Bearer [TOKEN_OCULTO]");

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    fullUrl, HttpMethod.GET, request, String.class);
            logger.info("Requisição de squads bem-sucedida");
            return response.getBody();
        } catch (Exception e) {
            logger.error("Erro na requisição de squads: {}", e.getMessage());
            throw e;
        }
    }

    public String getSquadLogTypes() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAuthToken().getAccessToken());
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl + SQUAD_LOGTYPE_PATH, HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    public String getSquadCategories() {
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
        logger.info("Token obtido para create: {}", token.getAccessToken() != null ? "Token presente" : "Token nulo");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        String fullUrl = apiUrl + SQUAD_LOG_PATH;
        logger.info("Fazendo requisição para criar squad log: {}", fullUrl);
        logger.info("Payload para create: {}", payload);
        logger.info("Authorization header: Bearer [TOKEN_OCULTO]");

        try {
            ResponseEntity<String> response = restTemplate.exchange(fullUrl, HttpMethod.POST, request, String.class);
            logger.info("Squad log criado com sucesso");
            return response;
        } catch (Exception e) {
            logger.error("Erro ao criar squad log: {}", e.getMessage());
            throw e;
        }
    }

    public ResponseEntity<String> updateSquadLog(Long squadLogId, String payload) {
        AuthTokenResponseDTO token = getAuthToken();
        logger.info("Token obtido para update: {}", token.getAccessToken() != null ? "Token presente" : "Token nulo");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<String> request = new HttpEntity<>(payload, headers);

        String fullUrl = apiUrl + SQUAD_LOG_PATH + "/" + squadLogId;
        logger.info("Fazendo requisição para atualizar squad log {}: {}", squadLogId, fullUrl);
        logger.info("Payload para update: {}", payload);
        logger.info("Authorization header: Bearer [TOKEN_OCULTO]");

        try {
            ResponseEntity<String> response = restTemplate.exchange(fullUrl, HttpMethod.PUT, request, String.class);
            logger.info("Squad log {} atualizado com sucesso", squadLogId);
            return response;
        } catch (Exception e) {
            logger.error("Erro ao atualizar squad log {}: {}", squadLogId, e.getMessage());
            throw e;
        }
    }

    public String getSquadLogAll() {
        AuthTokenResponseDTO token = getAuthToken();
        logger.info("Token obtido para squad logs: {}", token.getAccessToken() != null ? "Token presente" : "Token nulo");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token.getAccessToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> request = new HttpEntity<>(headers);

        String fullUrl = apiUrl + SQUAD_LOG_LIST_ALL_PATH;
        logger.info("Fazendo requisição para buscar todas as squads log: {}", fullUrl);
        logger.info("Authorization header: Bearer [TOKEN_OCULTO]");

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    fullUrl, HttpMethod.GET, request, String.class);
            logger.info("Requisição de squad logs bem-sucedida");
            return response.getBody();
        } catch (Exception e) {
            logger.error("Erro na requisição de squad logs: {}", e.getMessage());
            throw e;
        }
    }

    public String getSquadLogId(String id) {
        AuthTokenResponseDTO token = getAuthToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.getAccessToken());
        HttpEntity<Void> request = new HttpEntity<>(headers);

        String fullUrl = apiUrl + SQUAD_LOG_ID_PATH + id;
        logger.info("Fazendo requisição para buscar squads log: "+ id +" {}", fullUrl);

        ResponseEntity<String> response = restTemplate.exchange(
                fullUrl, HttpMethod.GET, request, String.class);
        return response.getBody();
    }
}
