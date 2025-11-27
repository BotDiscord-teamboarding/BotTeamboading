package com.meli.teamboardingBot.core.usecase.discorduserauthentication;

import com.meli.teamboardingBot.adapters.dto.AuthTokenResponseDTO;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DiscrdUserAuthenticationAbstract {

    protected final LoggerApiPort loggerApiPort;
    protected static final long TOKEN_EXPIRATION_TIME = 24 * 3600 * 1000L;
    protected final Map<String, UserAuthData> userTokens = new ConcurrentHashMap<>();

    public DiscrdUserAuthenticationAbstract(LoggerApiPort loggerApiPort) {
        this.loggerApiPort = loggerApiPort;
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
}
