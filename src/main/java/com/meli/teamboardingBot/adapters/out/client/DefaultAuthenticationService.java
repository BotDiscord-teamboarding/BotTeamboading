package com.meli.teamboardingBot.adapters.out.client;

import com.meli.teamboardingBot.core.ports.auth.api.GetApiTokenPort;
import com.meli.teamboardingBot.core.ports.auth.api.GetManualApiTokenPort;
import com.meli.teamboardingBot.adapters.dto.AuthTokenResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DefaultAuthenticationService implements GetApiTokenPort, GetManualApiTokenPort {
    private final ClientAuthBoarding authBoarding;
    @Autowired
    public DefaultAuthenticationService(ClientAuthBoarding authBoarding) {
        this.authBoarding = authBoarding;
    }

    @Override
    public AuthTokenResponseDTO getAuthToken() {
        return authBoarding.getToken();
    }

    @Override
    public AuthTokenResponseDTO getAuthManualToken(String username, String password) {
        return authBoarding.getToken(username, password);
    }
}
