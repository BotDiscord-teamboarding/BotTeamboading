package com.meli.teamboardingBot.core.ports.auth;

import com.meli.teamboardingBot.dto.AuthTokenResponseDTO;

public interface GetIsUserAuthenticatedPort {
    boolean isUserAuthenticated(String discordUserId);
}
