package com.meli.teamboardingBot.core.usecase.auth.oath;

import com.meli.teamboardingBot.adapters.dto.AuthTokenResponseDTO;
import com.meli.teamboardingBot.core.ports.auth.GetUserAuthenticateWithTokenPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

public class GetUserAuthenticateWithTokenUseCase extends UserTokenAbstract implements GetUserAuthenticateWithTokenPort {

    public GetUserAuthenticateWithTokenUseCase(LoggerApiPort loggerApiPort) {
        super(loggerApiPort);
    }
    @Override
    public AuthResponse authenticateUserWithToken(String discordUserId, String accessToken) {
        try {
            loggerApiPort.info("Autenticando usuário Discord via Google: {}", discordUserId);

            AuthTokenResponseDTO token = new AuthTokenResponseDTO(accessToken, "bearer", null);

            UserAuthData authData = new UserAuthData(
                    token,
                    System.currentTimeMillis() + TOKEN_EXPIRATION_TIME,
                    "google"
            );
            userTokens.put(discordUserId, authData);

            loggerApiPort.info("✅ Autenticação Google bem-sucedida para usuário Discord: {}", discordUserId);
            return new AuthResponse(true, "✅ Login via Google realizado com sucesso!");

        } catch (Exception e) {
            loggerApiPort.error("❌ Falha na autenticação Google para usuário Discord {}: {}", discordUserId, e.getMessage());
            return new AuthResponse(false, "❌ Falha na autenticação via Google.");
        }
    }
}
