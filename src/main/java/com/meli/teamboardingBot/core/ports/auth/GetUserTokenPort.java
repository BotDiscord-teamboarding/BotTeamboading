package com.meli.teamboardingBot.core.ports.auth;

import com.meli.teamboardingBot.adapters.dto.AuthTokenResponseDTO;

public interface GetUserTokenPort {
    AuthTokenResponseDTO getUserToken(String discordUserId);
}
