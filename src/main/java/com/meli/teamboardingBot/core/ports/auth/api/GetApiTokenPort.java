package com.meli.teamboardingBot.core.ports.auth.api;

import com.meli.teamboardingBot.dto.AuthTokenResponseDTO;

public interface GetApiTokenPort {
    AuthTokenResponseDTO getAuthToken();
}
