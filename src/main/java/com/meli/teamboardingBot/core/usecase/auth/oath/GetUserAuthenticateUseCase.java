package com.meli.teamboardingBot.core.usecase.auth.oath;

import com.meli.teamboardingBot.core.ports.auth.GetUserAuthenticatePort;
import com.meli.teamboardingBot.core.ports.auth.api.GetManualApiTokenPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import com.meli.teamboardingBot.adapters.dto.AuthTokenResponseDTO;

public class GetUserAuthenticateUseCase implements GetUserAuthenticatePort {

    private final LoggerApiPort loggerApiPort;
    private final GetManualApiTokenPort getManualApiTokenPort;

    public GetUserAuthenticateUseCase(LoggerApiPort logger, GetManualApiTokenPort getManualApiTokenPort) {
        this.loggerApiPort = logger;
        this.getManualApiTokenPort = getManualApiTokenPort;
    }

    public UserTokenAbstract.AuthResponse authenticateUser(String discordUserId, String username, String password) {
        try {
            loggerApiPort.info("Tentando autenticar usuário Discord: {}", discordUserId);
            AuthTokenResponseDTO token = getManualApiTokenPort.getAuthManualToken(username, password);
            if (token != null && token.getAccessToken() != null) {
                UserTokenManager.storeUserToken(discordUserId, token, "manual");

                loggerApiPort.info("Autenticação bem-sucedida para usuário Discord: {}", discordUserId);
                return new UserTokenAbstract.AuthResponse(true, "✅ Login realizado com sucesso! Agora você pode usar o comando /squad-log.");
            } else {
                loggerApiPort.warn("Token não recebido para usuário Discord: {}", discordUserId);
                return new UserTokenAbstract.AuthResponse(false, "❌ Falha na autenticação. Token não recebido.");
            }

        } catch (Exception e) {
            loggerApiPort.error("Falha na autenticação para usuário Discord {}: {}", discordUserId, e.getMessage());
            return new UserTokenAbstract.AuthResponse(false, "❌ Falha na autenticação. Verifique suas credenciais e tente novamente.");
        }
    }
}
