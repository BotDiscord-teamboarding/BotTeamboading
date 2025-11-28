package com.meli.teamboardingBot.core.usecase.auth.oath;

import com.meli.teamboardingBot.adapters.dto.AuthTokenResponseDTO;
import com.meli.teamboardingBot.core.ports.auth.GetUserAuthenticateWithTokenPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

public class GetUserAuthenticateWithTokenUseCase implements GetUserAuthenticateWithTokenPort {

    private final LoggerApiPort loggerApiPort;

    public GetUserAuthenticateWithTokenUseCase(LoggerApiPort loggerApiPort) {
        this.loggerApiPort = loggerApiPort;
    }
    @Override
    public UserTokenAbstract.AuthResponse authenticateUserWithToken(String discordUserId, String accessToken) {
        try {
            loggerApiPort.info("Autenticando usuário Discord via Google: {}", discordUserId);

            AuthTokenResponseDTO token = new AuthTokenResponseDTO(accessToken, "bearer", null);
            UserTokenManager.storeUserToken(discordUserId, token, "google");

            loggerApiPort.info("✅ Autenticação Google bem-sucedida para usuário Discord: {}", discordUserId);
            return new UserTokenAbstract.AuthResponse(true, "✅ Login via Google realizado com sucesso!");

        } catch (Exception e) {
            loggerApiPort.error("❌ Falha na autenticação Google para usuário Discord {}: {}", discordUserId, e.getMessage());
            return new UserTokenAbstract.AuthResponse(false, "❌ Falha na autenticação via Google.");
        }
    }
}
