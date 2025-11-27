package com.meli.teamboardingBot.core.usecase.auth.oath;

import com.meli.teamboardingBot.core.ports.auth.GetUserAuthenticatePort;
import com.meli.teamboardingBot.core.ports.auth.api.GetManualApiTokenPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import com.meli.teamboardingBot.adapters.dto.AuthTokenResponseDTO;

public class GetUserAuthenticateUseCase extends UserTokenAbstract implements GetUserAuthenticatePort {

    private GetManualApiTokenPort getManualApiTokenPort;

    public GetUserAuthenticateUseCase(LoggerApiPort logger, GetManualApiTokenPort getManualApiTokenPort) {
        super(logger);
        this.getManualApiTokenPort = getManualApiTokenPort;
    }

    public AuthResponse authenticateUser(String discordUserId, String username, String password) {
        try {
            loggerApiPort.info("Tentando autenticar usuário Discord: {}", discordUserId);
            AuthTokenResponseDTO token = getManualApiTokenPort.getAuthManualToken(username, password);
            if (token != null && token.getAccessToken() != null) {
                UserAuthData authData = new UserAuthData(
                        token,
                        System.currentTimeMillis() + TOKEN_EXPIRATION_TIME,
                        "manual"
                );
                userTokens.put(discordUserId, authData);

                loggerApiPort.info("Autenticação bem-sucedida para usuário Discord: {}", discordUserId);
                return new AuthResponse(true, "✅ Login realizado com sucesso! Agora você pode usar o comando /squad-log.");
            } else {
                loggerApiPort.warn("Token não recebido para usuário Discord: {}", discordUserId);
                return new AuthResponse(false, "❌ Falha na autenticação. Token não recebido.");
            }

        } catch (Exception e) {
            loggerApiPort.error("Falha na autenticação para usuário Discord {}: {}", discordUserId, e.getMessage());
            return new AuthResponse(false, "❌ Falha na autenticação. Verifique suas credenciais e tente novamente.");
        }
    }
}
