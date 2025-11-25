package com.meli.teamboardingBot.core.usecase.auth;

import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import com.meli.teamboardingBot.dto.AuthTokenResponseDTO;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserTokenAbstract {

    protected final LoggerApiPort logger;
    protected final Map<String, UserTokenAbstract.UserAuthData> userTokens = new ConcurrentHashMap<>();

    public UserTokenAbstract(LoggerApiPort logger) {
        this.logger = logger;
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
