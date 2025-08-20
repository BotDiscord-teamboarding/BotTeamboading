package com.meli.teamboardingBot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class ApiService {

    @Value("${api.squad.url}")
    private String squadUrl;

    @Value("${api.squad.logtype}")
    private String squadLogTypeUrl;

    @Value("${api.squad.categories}")
    private String squadCategoriesUrl;

    @Autowired
    private AuthService authService;

    public String getSquads() {
        String token = authService.getToken();
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                squadUrl + "/all?obi_squads=false", HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    public String getSquadLogTypes() {
        String token = authService.getToken();
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                squadLogTypeUrl, HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    public String getSquadCategories() {
        String token = authService.getToken();
        Integer clientId = authService.getUserClientId();
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = squadCategoriesUrl + "?client_id=" + clientId;
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, request, String.class);
        return response.getBody();
    }


    public ResponseEntity<String> createSquadLog(String payload) {
        String token = authService.getToken();
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<String> request = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                squadUrl.replace("/squads", "/squad_logs"),
                HttpMethod.POST, request, String.class);
        return restTemplate.exchange(
                squadUrl.replace("/squads", "/squad_logs"),
                HttpMethod.POST, request, String.class);
    }
}