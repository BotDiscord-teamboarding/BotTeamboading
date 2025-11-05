package com.meli.teamboardingBot.service;

import com.meli.teamboardingBot.client.ClientAuthBoarding;
import com.meli.teamboardingBot.dto.AuthTokenResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class DiscordUserAuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(DiscordUserAuthenticationService.class);
    private static final long TOKEN_EXPIRATION_TIME = 24 * 3600 * 1000L;
    
    private final ClientAuthBoarding authClient;
    private final Map<String, UserAuthData> userTokens = new ConcurrentHashMap<>();
    
    public DiscordUserAuthenticationService(ClientAuthBoarding authClient) {
        this.authClient = authClient;
    }
    

    public boolean isUserAuthenticated(String discordUserId) {
        UserAuthData authData = userTokens.get(discordUserId);
        if (authData == null) {
            return false;
        }
        
        if (System.currentTimeMillis() >= authData.expirationTime) {
            userTokens.remove(discordUserId);
            logger.info("Token expirado para usuário Discord: {}", discordUserId);
            return false;
        }
        return true;
    }
    

    public AuthTokenResponseDTO getUserToken(String discordUserId) {
        if (!isUserAuthenticated(discordUserId)) {
            return null;
        }
        return userTokens.get(discordUserId).token;
    }

    public AuthResponse authenticateUser(String discordUserId, String username, String password) {
        try {
            logger.info("Tentando autenticar usuário Discord: {}", discordUserId);
            AuthTokenResponseDTO token = authClient.getToken(username, password);
            if (token != null && token.getAccessToken() != null) {
                UserAuthData authData = new UserAuthData(
                    token,
                    System.currentTimeMillis() + TOKEN_EXPIRATION_TIME
                );
                userTokens.put(discordUserId, authData);
                
                logger.info("Autenticação bem-sucedida para usuário Discord: {}", discordUserId);
                return new AuthResponse(true, "✅ Login realizado com sucesso! Agora você pode usar o comando /squad-log.");
            } else {
                logger.warn("Token não recebido para usuário Discord: {}", discordUserId);
                return new AuthResponse(false, "❌ Falha na autenticação. Token não recebido.");
            }
            
        } catch (Exception e) {
            logger.error("Falha na autenticação para usuário Discord {}: {}", discordUserId, e.getMessage());
            return new AuthResponse(false, "❌ Falha na autenticação. Verifique suas credenciais e tente novamente.");
        }
    }
    

    public void logoutUser(String discordUserId) {
        userTokens.remove(discordUserId);
        logger.info("Logout realizado para usuário Discord: {}", discordUserId);
    }
    

    private static class UserAuthData {
        final AuthTokenResponseDTO token;
        final long expirationTime;
        
        UserAuthData(AuthTokenResponseDTO token, long expirationTime) {
            this.token = token;
            this.expirationTime = expirationTime;
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
}
