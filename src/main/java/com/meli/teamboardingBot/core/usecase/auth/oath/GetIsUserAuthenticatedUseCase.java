package com.meli.teamboardingBot.core.usecase.auth.oath;

import com.meli.teamboardingBot.core.ports.auth.GetIsUserAuthenticatedPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

public class GetIsUserAuthenticatedUseCase implements GetIsUserAuthenticatedPort {

    private final LoggerApiPort loggerApiPort;

    public GetIsUserAuthenticatedUseCase(LoggerApiPort logger) {
        this.loggerApiPort = logger;
    }

    public boolean isUserAuthenticated(String discordUserId) {
        UserTokenManager.UserAuthData authData = UserTokenManager.getUserToken(discordUserId);
        if (authData == null) {
            loggerApiPort.info("Usuário não autenticado ou token expirado: {}", discordUserId);
            return false;
        }
        return true;
    }
}


