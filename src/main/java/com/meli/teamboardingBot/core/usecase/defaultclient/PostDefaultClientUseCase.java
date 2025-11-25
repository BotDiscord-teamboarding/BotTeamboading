package com.meli.teamboardingBot.core.usecase.defaultclient;

import com.meli.teamboardingBot.core.ports.auth.GetUserTokenPort;
import com.meli.teamboardingBot.core.ports.auth.api.GetApiTokenPort;
import com.meli.teamboardingBot.core.ports.defaultclient.PostDefaultClientPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import com.meli.teamboardingBot.core.ports.rest.RestPort;

public class PostDefaultClientUseCase extends DefaultClientAbstract implements PostDefaultClientPort {

    private String apiUrl;
    private RestPort restPort;

    public PostDefaultClientUseCase(LoggerApiPort logger, GetApiTokenPort authService, GetUserTokenPort getUserTokenPort, String apiUrl, RestPort restPort) {
        super(logger, authService, getUserTokenPort);
        this.apiUrl = apiUrl;
        this.restPort = restPort;
    }

    @Override
    public String post(String endpoint, String payload) {
        String token = getAuthToken();
        String fullUrl = apiUrl + endpoint;
        logger.info("POST request to: {}", fullUrl);
        try {
            return restPort.postExchange(fullUrl, token, payload);
        } catch (Exception e) {
            logger.error("POST request failed: {}", e.getMessage());
            throw e;
        }
    }

}
