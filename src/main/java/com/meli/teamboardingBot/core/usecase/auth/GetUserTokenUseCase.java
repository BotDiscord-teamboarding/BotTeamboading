package com.meli.teamboardingBot.core.usecase.auth;

import com.meli.teamboardingBot.core.ports.auth.GetIsUserAuthenticatedPort;
import com.meli.teamboardingBot.core.ports.auth.GetUserTokenPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import com.meli.teamboardingBot.adapters.dto.AuthTokenResponseDTO;

public class GetUserTokenUseCase extends UserTokenAbstract implements GetUserTokenPort {

    private GetIsUserAuthenticatedPort isUserAuthenticated;

    public GetUserTokenUseCase(LoggerApiPort logger) {
        super(logger);
    }

    @Override
    public AuthTokenResponseDTO getUserToken(String discordUserId) {
        if (!isUserAuthenticated.isUserAuthenticated(discordUserId)) {
            return null;
        }
        return userTokens.get(discordUserId).token;
    }
}
