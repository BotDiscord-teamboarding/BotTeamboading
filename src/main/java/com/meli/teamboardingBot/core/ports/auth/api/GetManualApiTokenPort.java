package com.meli.teamboardingBot.core.ports.auth.api;

import com.meli.teamboardingBot.adapters.dto.AuthTokenResponseDTO;

public interface GetManualApiTokenPort {
    AuthTokenResponseDTO getAuthManualToken(String username, String password);
}
