package com.meli.teamboardingBot.client;
import com.meli.teamboardingBot.constants.ApiEndpoints;
import com.meli.teamboardingBot.service.AuthenticationService;
import com.meli.teamboardingBot.service.HttpClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
@Component
public class ClientBoarding {
    private final AuthenticationService authService;
    private final HttpClientService httpClient;
    private final Integer limit = 15;
    @Autowired
    public ClientBoarding(AuthenticationService authService, HttpClientService httpClient) {
        this.authService = authService;
        this.httpClient = httpClient;
    }
    public String getSquads() {
        return httpClient.get(ApiEndpoints.SQUAD_LIST);
    }
    public String getSquadLogTypes() {
        return httpClient.get(ApiEndpoints.SQUAD_LOG_TYPES);
    }
    public String getSquadCategories() {
        String queryParams = "client_id=" + authService.getClientId();
        return httpClient.get(ApiEndpoints.SQUAD_CATEGORIES, queryParams);
    }
    public ResponseEntity<String> createSquadLog(String payload) {
        return httpClient.post(ApiEndpoints.SQUAD_LOG, payload);
    }
    public ResponseEntity<String> updateSquadLog(Long squadLogId, String payload) {
        String endpoint = ApiEndpoints.SQUAD_LOG + "/" + squadLogId;
        return httpClient.put(endpoint, payload);
    }
    public String getSquadLogAll() {
        return httpClient.get(ApiEndpoints.SQUAD_LOG_LIST_ALL);
    }
    
    public String getSquadLogAll(int page, int limit) {
        int offset = (page - 1) * limit;
        String endpoint = ApiEndpoints.buildSquadLogListUrl(offset, limit);
        return httpClient.get(endpoint);
    }
    public String getSquadLogId(String id) {
        String endpoint = ApiEndpoints.SQUAD_LOG_BY_ID + id;
        return httpClient.get(endpoint);
    }
}
