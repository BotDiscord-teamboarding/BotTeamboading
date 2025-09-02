package com.meli.teamboardingBot.service;

import com.meli.teamboardingBot.dto.AuthTokenResponseDTO;

public interface AuthenticationService {
    AuthTokenResponseDTO getAuthToken();
    Long getClientId();
}
