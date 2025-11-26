package com.meli.teamboardingBot.core.usecase.auth;

import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import com.meli.teamboardingBot.adapters.dto.AuthTokenResponseDTO;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserTokenAbstract {

    protected final LoggerApiPort loggerApiPort;
    protected static final long TOKEN_EXPIRATION_TIME = 24 * 3600 * 1000L;
    protected final Map<String, UserTokenAbstract.UserAuthData> userTokens = new ConcurrentHashMap<>();

    public UserTokenAbstract(LoggerApiPort loggerApiPort) {
        this.loggerApiPort = loggerApiPort;
    }

    protected static class UserAuthData {
        final AuthTokenResponseDTO token;
        final long expirationTime;
        final String authMethod;

        UserAuthData(AuthTokenResponseDTO token, long expirationTime, String authMethod) {
            this.token = token;
            this.expirationTime = expirationTime;
            this.authMethod = authMethod;

        }
    }

    public static class AuthResponse {
        private final boolean success;
        private final String message;

        public AuthResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
    public String getAuthMethod(String discordUserId) {
        UserAuthData authData = userTokens.get(discordUserId);
        if (authData == null) {
            return null;
        }
        return authData.authMethod;
    }
}
