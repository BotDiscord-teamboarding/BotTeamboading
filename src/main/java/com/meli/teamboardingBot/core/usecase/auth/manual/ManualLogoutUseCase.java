package com.meli.teamboardingBot.core.usecase.auth.manual;

import com.meli.teamboardingBot.core.ports.discorduserauthentication.LogoutDiscordUserPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

public class ManualLogoutUseCase extends ManualAuthenticationAbstract implements LogoutDiscordUserPort {

    public ManualLogoutUseCase(LoggerApiPort loggerApiPort) {
        super(loggerApiPort);
    }

    @Override
    public void logoutUser(String discordUserId) {
        userTokens.remove(discordUserId);
        loggerApiPort.info("Logout realizado para usuário Discord: {}", discordUserId);
    }
}
