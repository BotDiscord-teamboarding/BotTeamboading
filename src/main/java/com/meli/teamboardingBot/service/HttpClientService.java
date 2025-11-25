package com.meli.teamboardingBot.service;
import org.springframework.http.ResponseEntity;
public interface HttpClientService {
    String get(String endpoint);
    String get(String endpoint, String queryParams);
    ResponseEntity<String> post(String endpoint, String payload);
    ResponseEntity<String> put(String endpoint, String payload);
}
