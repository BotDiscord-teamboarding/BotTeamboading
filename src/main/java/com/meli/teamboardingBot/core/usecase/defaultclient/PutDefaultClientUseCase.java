package com.meli.teamboardingBot.core.usecase.defaultclient;

import com.meli.teamboardingBot.core.ports.auth.GetUserTokenPort;
import com.meli.teamboardingBot.core.ports.auth.api.GetApiTokenPort;
import com.meli.teamboardingBot.core.ports.defaultclient.GetDefaultClientPort;
import com.meli.teamboardingBot.core.ports.defaultclient.GetDefaultClientWithParamPort;
import com.meli.teamboardingBot.core.ports.defaultclient.PutDefaultClientPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import com.meli.teamboardingBot.core.ports.rest.RestPort;

public class PutDefaultClientUseCase extends DefaultClientAbstract implements PutDefaultClientPort {

    private String apiUrl;

    private RestPort restPort;

    public PutDefaultClientUseCase(LoggerApiPort logger, GetApiTokenPort authService, GetUserTokenPort getUserTokenPort, String apiUrl, RestPort restPort) {
        super(logger, authService, getUserTokenPort);
        this.apiUrl = apiUrl;
        this.restPort = restPort;
    }

    @Override
    public String put(String endpoint, String payload) {
        String token = getAuthToken();
        String fullUrl = apiUrl + endpoint;
        logger.info("PUT request to: {}", fullUrl);
        try {
            return restPort.putExchange(fullUrl, token, payload);
        } catch (Exception e) {
            logger.error("PUT request failed: {}", e.getMessage());
            throw e;
        }
    }
}
