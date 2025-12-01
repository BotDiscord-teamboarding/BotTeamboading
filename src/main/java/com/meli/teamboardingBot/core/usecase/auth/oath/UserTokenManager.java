package com.meli.teamboardingBot.core.usecase.auth.oath;

import com.meli.teamboardingBot.adapters.dto.AuthTokenResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserTokenManager {
    private static final Logger logger = LoggerFactory.getLogger(UserTokenManager.class);
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
        long currentTime = System.currentTimeMillis();
        long expirationTime = currentTime + TOKEN_EXPIRATION_TIME;
        
        UserAuthData authData = new UserAuthData(
                token,
                expirationTime,
                authMethod
        );
        userTokens.put(discordUserId, authData);
        
        logger.info("🔐 Token armazenado para usuário: {}", discordUserId);
        logger.info("   Método: {}", authMethod);
        logger.info("   Tempo atual: {}", currentTime);
        logger.info("   Expira em: {}", expirationTime);
        logger.info("   Duração: {} horas", TOKEN_EXPIRATION_TIME / (3600 * 1000));
        logger.info("   Total de tokens armazenados: {}", userTokens.size());
    }

    public static UserAuthData getUserToken(String discordUserId) {
        long currentTime = System.currentTimeMillis();
        UserAuthData authData = userTokens.get(discordUserId);
        
        logger.info("🔍 Buscando token para usuário: {}", discordUserId);
        logger.info("   Tempo atual: {}", currentTime);
        
        if (authData == null) {
            logger.warn("   ❌ Token NÃO ENCONTRADO no mapa (usuário nunca autenticou ou token foi removido)");
            logger.info("   Total de tokens no mapa: {}", userTokens.size());
            logger.info("   Usuários no mapa: {}", userTokens.keySet());
            return null;
        }
        
        logger.info("   ✅ Token ENCONTRADO no mapa");
        logger.info("   Método de autenticação: {}", authData.authMethod);
        logger.info("   Expira em: {}", authData.expirationTime);
        logger.info("   Tempo restante: {} ms", (authData.expirationTime - currentTime));
        
        if (currentTime > authData.expirationTime) {
            logger.warn("   ⏰ Token EXPIRADO! Removendo...");
            userTokens.remove(discordUserId);
            return null;
        }
        
        logger.info("   ✅ Token VÁLIDO e não expirado");
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
        UserAuthData removed = userTokens.remove(discordUserId);
        if (removed != null) {
            logger.info("🗑️ Token removido para usuário: {}", discordUserId);
        } else {
            logger.warn("⚠️ Tentativa de remover token inexistente para usuário: {}", discordUserId);
        }
    }
}
