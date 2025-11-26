package com.meli.teamboardingBot.core.usecase.auth;

import com.meli.teamboardingBot.core.ports.auth.GetIsUserAuthenticatedPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

public class GetIsUserAuthenticatedUseCase extends UserTokenAbstract implements GetIsUserAuthenticatedPort {

    public GetIsUserAuthenticatedUseCase(LoggerApiPort logger) {
        super(logger);
    }

    public boolean isUserAuthenticated(String discordUserId) {
        UserTokenAbstract.UserAuthData authData = userTokens.get(discordUserId);
        if (authData == null) {
            return false;
        }

        if (System.currentTimeMillis() >= authData.expirationTime) {
            userTokens.remove(discordUserId);
            loggerApiPort.info("Token expirado para usuário Discord: {}", discordUserId);
            return false;
        }
        return true;
    }
}


