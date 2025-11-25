package com.meli.teamboardingBot.service;
import com.meli.teamboardingBot.adapters.dto.AuthTokenResponseDTO;
public interface AuthenticationService {
    AuthTokenResponseDTO getAuthToken();
    Long getClientId();
}
