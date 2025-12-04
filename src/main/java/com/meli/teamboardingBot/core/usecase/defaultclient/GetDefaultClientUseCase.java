package com.meli.teamboardingBot.core.usecase.defaultclient;

import com.meli.teamboardingBot.core.ports.auth.GetUserTokenPort;
import com.meli.teamboardingBot.core.ports.auth.api.GetApiTokenPort;
import com.meli.teamboardingBot.core.ports.defaultclient.GetDefaultClientPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import com.meli.teamboardingBot.core.ports.rest.RestPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

public class GetDefaultClientUseCase extends DefaultClientAbstract implements GetDefaultClientPort {

    private String apiUrl;

    private RestPort restPort;

    public GetDefaultClientUseCase(LoggerApiPort logger, GetApiTokenPort authService, GetUserTokenPort getUserTokenPort, String apiUrl, RestPort restPort) {
        super(logger, authService, getUserTokenPort);
        this.apiUrl = apiUrl;
        this.restPort = restPort;
    }

    @Override
    public String get(String endpoint) {
        String token = getAuthToken();
        String fullUrl = apiUrl + endpoint;
        logger.info("GET request to: {}", fullUrl);
        try {
            return restPort.getExchangeWithStringType(fullUrl, token);
        } catch (Exception e) {
            logger.error("GET request failed: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public String post( String endpoint, String payload) {
        String token = getAuthToken();
        String fullUrl = apiUrl + endpoint;

        try {
            return restPort.postExchange(fullUrl, token, payload);

        } catch (Exception e) {
            logger.error("POST request failed: {}", e.getMessage());
            throw e;
        }
    }
    @Override
    public String put(String endpoint, String payload) {
        String token = getAuthToken();
        String fullUrl = apiUrl + endpoint;
        try {
            return  restPort.putExchange(fullUrl, token, payload);
        } catch (Exception e) {

            throw e;
        }

    }

    @Override
    public String get(String endpoint, String queryParams) {
        String fullEndpoint = endpoint + "?" + queryParams;
        return get(fullEndpoint);
    }






}
