package com.meli.teamboardingBot.service.impl;

import com.meli.teamboardingBot.client.ClientAuthBoarding;
import com.meli.teamboardingBot.dto.AuthTokenResponseDTO;
import com.meli.teamboardingBot.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DefaultAuthenticationService implements AuthenticationService {
    
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
    public Long getClientId() {
        return getAuthToken().getUser().getId();
    }
}
