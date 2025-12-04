package com.meli.teamboardingBot.adapters.out.client;
import com.meli.teamboardingBot.adapters.out.client.constants.ApiEndpoints;
import com.meli.teamboardingBot.core.ports.defaultclient.GetDefaultClientPort;
import com.meli.teamboardingBot.core.ports.defaultclient.GetDefaultClientWithParamPort;
import com.meli.teamboardingBot.core.ports.defaultclient.PostDefaultClientPort;
import com.meli.teamboardingBot.core.ports.defaultclient.PutDefaultClientPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
@Component
public class ClientBoarding {
    private final GetDefaultClientPort getDefaultClientPort;
    private final GetDefaultClientWithParamPort getDefaultClientWithParamPort;
    private final PostDefaultClientPort postDefaultClientPort;
    private final PutDefaultClientPort putDefaultClientPort;
    
    private final Integer limit = 15;
    
    @Autowired
    public ClientBoarding(GetDefaultClientPort getDefaultClientPort, GetDefaultClientWithParamPort getDefaultClientWithParamPort, PostDefaultClientPort postDefaultClientPort, PutDefaultClientPort putDefaultClientPort) {
        this.getDefaultClientPort = getDefaultClientPort;
        this.getDefaultClientWithParamPort = getDefaultClientWithParamPort;
        this.postDefaultClientPort = postDefaultClientPort;
        this.putDefaultClientPort = putDefaultClientPort;
    }
    
    public String getSquads() {
        return getDefaultClientPort.get(ApiEndpoints.SQUAD_LIST);
    }
    
    public String getSquadLogTypes() {
        return getDefaultClientPort.get(ApiEndpoints.SQUAD_LOG_TYPES);
    }
    
    public String getSquadCategories() {
        return getDefaultClientPort.get(ApiEndpoints.SQUAD_CATEGORIES);
    }
    
    public ResponseEntity<String> createSquadLog(String payload) {
        String result = postDefaultClientPort.post(ApiEndpoints.SQUAD_LOG, payload);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
    
    public ResponseEntity<String> updateSquadLog(Long squadLogId, String payload) {
        String endpoint = ApiEndpoints.SQUAD_LOG + "/" + squadLogId;
        String result = putDefaultClientPort.put(endpoint, payload);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    public String getSquadLogAll() {
        return getDefaultClientPort.get(ApiEndpoints.SQUAD_LOG_LIST_ALL);
    }
    
    public String getSquadLogAll(int page, int limit) {
        int offset = (page - 1) * limit;
        String endpoint = ApiEndpoints.buildSquadLogListUrl(offset, limit);
        return getDefaultClientPort.get(endpoint);
    }
    public String getSquadLogId(String id) {
        String endpoint = ApiEndpoints.SQUAD_LOG_BY_ID + id;
        return getDefaultClientPort.get(endpoint);
    }
    
    public String getUsersBySquad(String squadId) {
        String endpoint = ApiEndpoints.SQUAD_BASE + "/" + squadId + "/users";
        return getDefaultClientPort.get(endpoint);
    }
}
