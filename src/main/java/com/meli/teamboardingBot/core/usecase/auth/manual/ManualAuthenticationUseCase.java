package com.meli.teamboardingBot.core.usecase.auth.manual;

import com.meli.teamboardingBot.adapters.dto.AuthTokenResponseDTO;
import com.meli.teamboardingBot.core.ports.discorduserauthentication.DiscordUserAuthenticationPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import com.meli.teamboardingBot.core.usecase.auth.oath.UserTokenManager;

public class ManualAuthenticationUseCase extends ManualAuthenticationAbstract implements DiscordUserAuthenticationPort {

    public ManualAuthenticationUseCase(LoggerApiPort loggerApiPort) {
        super(loggerApiPort);
    }

    public AuthResponse authenticateUserWithToken(String discordUserId, String accessToken) {
        try {
            loggerApiPort.info("Autenticando usuário Discord via Google: {}", discordUserId);
            loggerApiPort.info("   [ManualAuthenticationUseCase] Usando UserTokenManager para armazenar token");

            AuthTokenResponseDTO token = new AuthTokenResponseDTO(accessToken, "bearer", null);
            
            UserTokenManager.storeUserToken(discordUserId, token, "google");

            loggerApiPort.info("✅ Autenticação Google bem-sucedida para usuário Discord: {}", discordUserId);
            return new AuthResponse(true, "✅ Login via Google realizado com sucesso!");

        } catch (Exception e) {
            loggerApiPort.error("❌ Falha na autenticação Google para usuário Discord {}: {}", discordUserId, e.getMessage());
            loggerApiPort.error("   Stack trace: ", e);
            return new AuthResponse(false, "❌ Falha na autenticação via Google.");
        }
    }
}
