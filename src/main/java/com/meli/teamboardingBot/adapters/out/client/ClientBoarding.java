package com.meli.teamboardingBot.adapters.out.client;
import com.meli.teamboardingBot.adapters.out.client.constants.ApiEndpoints;
import com.meli.teamboardingBot.service.HttpClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
@Component
public class ClientBoarding {
    private final HttpClientService httpClient;
    private final Integer limit = 15;
    @Autowired
    public ClientBoarding(HttpClientService httpClient) {
        this.httpClient = httpClient;
    }
    public String getSquads() {
        return httpClient.get(ApiEndpoints.SQUAD_LIST);
    }
    public String getSquadLogTypes() {
        return httpClient.get(ApiEndpoints.SQUAD_LOG_TYPES);
    }
    public String getSquadCategories() {
        return httpClient.get(ApiEndpoints.SQUAD_CATEGORIES);
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
    
    public String getUsersBySquad(String squadId) {
        String endpoint = ApiEndpoints.SQUAD_BASE + "/" + squadId + "/users";
        return httpClient.get(endpoint);
    }
}
