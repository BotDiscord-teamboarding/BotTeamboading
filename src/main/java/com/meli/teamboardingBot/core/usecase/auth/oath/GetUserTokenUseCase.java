package com.meli.teamboardingBot.core.usecase.auth.oath;

import com.meli.teamboardingBot.core.ports.auth.GetIsUserAuthenticatedPort;
import com.meli.teamboardingBot.core.ports.auth.GetUserTokenPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import com.meli.teamboardingBot.adapters.dto.AuthTokenResponseDTO;

public class GetUserTokenUseCase extends UserTokenAbstract implements GetUserTokenPort {

    private final GetIsUserAuthenticatedPort isUserAuthenticated;

    public GetUserTokenUseCase(LoggerApiPort logger, GetIsUserAuthenticatedPort isUserAuthenticated) {
        super(logger);
        this.isUserAuthenticated = isUserAuthenticated;
    }

    @Override
    public AuthTokenResponseDTO getUserToken(String discordUserId) {
        if (!isUserAuthenticated.isUserAuthenticated(discordUserId)) {
            return null;
        }
        return userTokens.get(discordUserId).token;
    }
}
