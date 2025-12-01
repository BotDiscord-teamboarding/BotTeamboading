package com.meli.teamboardingBot.core.usecase.auth.oath;

import com.meli.teamboardingBot.adapters.dto.AuthTokenResponseDTO;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserTokenManager {
    private static final Map<String, UserAuthData> userTokens = new ConcurrentHashMap<>();
    private static final long TOKEN_EXPIRATION_TIME = 24 * 3600 * 1000L;

    public static class UserAuthData {
        final AuthTokenResponseDTO token;
        final long expirationTime;
        final String authMethod;

        public UserAuthData(AuthTokenResponseDTO token, long expirationTime, String authMethod) {
            this.token = token;
            this.expirationTime = expirationTime;
            this.authMethod = authMethod;
        }
    }

    public static void storeUserToken(String discordUserId, AuthTokenResponseDTO token, String authMethod) {
        UserAuthData authData = new UserAuthData(
                token,
                System.currentTimeMillis() + TOKEN_EXPIRATION_TIME,
                authMethod
        );
        userTokens.put(discordUserId, authData);
    }

    public static UserAuthData getUserToken(String discordUserId) {
        UserAuthData authData = userTokens.get(discordUserId);
        if (authData != null && System.currentTimeMillis() > authData.expirationTime) {
            userTokens.remove(discordUserId);
            return null;
        }
        return authData;
    }

    public static boolean isUserAuthenticated(String discordUserId) {
        return getUserToken(discordUserId) != null;
    }

    public static String getAuthMethod(String discordUserId) {
        UserAuthData authData = getUserToken(discordUserId);
        return authData != null ? authData.authMethod : null;
    }

    public static void removeUserToken(String discordUserId) {
        userTokens.remove(discordUserId);
    }
}
