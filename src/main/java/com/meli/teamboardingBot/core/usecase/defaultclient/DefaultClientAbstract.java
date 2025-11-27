package com.meli.teamboardingBot.core.usecase.defaultclient;

import com.meli.teamboardingBot.core.ports.auth.GetUserTokenPort;
import com.meli.teamboardingBot.core.ports.auth.api.GetApiTokenPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import com.meli.teamboardingBot.core.context.UserContext;
import com.meli.teamboardingBot.adapters.dto.AuthTokenResponseDTO;

public class DefaultClientAbstract {

    protected final LoggerApiPort logger;
    protected final GetApiTokenPort apiToken;
    protected final GetUserTokenPort userToken;

    public DefaultClientAbstract(LoggerApiPort logger, GetApiTokenPort apiToken, GetUserTokenPort userToken) {
        this.logger = logger;
        this.apiToken = apiToken;
        this.userToken = userToken;
    }

    protected String getAuthToken() {
        String discordUserId = UserContext.getCurrentUserId();
        if (discordUserId != null) {
            AuthTokenResponseDTO token = userToken.getUserToken(discordUserId);
            if (userToken != null) {
                logger.debug("Usando token do usuário Discord: {}", discordUserId);
                return token.getAccessToken();
            }
        }
        logger.debug("Usando token padrão (credenciais do application.properties)");
        return apiToken.getAuthToken().getAccessToken();
    }

}
