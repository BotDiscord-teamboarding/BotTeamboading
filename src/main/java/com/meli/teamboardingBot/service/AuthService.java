package com.meli.teamboardingBot.service;

import com.meli.teamboardingBot.client.ClientAuthBoarding;
import com.meli.teamboardingBot.dto.AuthTokenResponseDTO;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
    private final ClientAuthBoarding authClient;
    private final Map<String, AuthState> authStates = new HashMap<>();
    private final Map<String, String> userCredentials = new HashMap<>();

    public AuthService(ClientAuthBoarding authClient) {
        this.authClient = authClient;
    }

    public void startAuthFlow(String userId) {
        authStates.put(userId, AuthState.AWAITING_USERNAME);
    }

    public AuthState getAuthState(String userId) {
        return authStates.getOrDefault(userId, AuthState.NOT_AUTHENTICATED);
    }

    public AuthResponse handleAuthStep(String userId, String input) {
        AuthState currentState = getAuthState(userId);
        
        switch (currentState) {
            case AWAITING_USERNAME:
                userCredentials.put(userId + ":username", input);
                authStates.put(userId, AuthState.AWAITING_PASSWORD);
                return new AuthResponse("Agora, por favor, insira sua senha:", false);
                
            case AWAITING_PASSWORD:
                String username = userCredentials.get(userId + ":username");
                userCredentials.put(userId + ":password", input);
                
                try {
                    String originalUser = authClient.getUsername();
                    String originalPass = authClient.getPassword();
                    
                    authClient.setCredentials(username, input);
                    AuthTokenResponseDTO token = authClient.getToken();
                    
                    authStates.put(userId, AuthState.AUTHENTICATED);
                    return new AuthResponse("✅ Autenticação realizada com sucesso! Agora você pode usar os comandos do bot.", true);
                    
                } catch (Exception e) {
                    authStates.remove(userId);
                    userCredentials.remove(userId + ":username");
                    userCredentials.remove(userId + ":password");
                    return new AuthResponse("❌ Credenciais inválidas. Use o comando /login para tentar novamente.", false);
                }
                
            default:
                return new AuthResponse("Por favor, use o comando /login para começar a autenticação.", false);
        }
    }

    public boolean isAuthenticated(String userId) {
        return authStates.getOrDefault(userId, AuthState.NOT_AUTHENTICATED) == AuthState.AUTHENTICATED;
    }

    public enum AuthState {
        NOT_AUTHENTICATED,
        AWAITING_USERNAME,
        AWAITING_PASSWORD,
        AUTHENTICATED
    }

    public static class AuthResponse {
        private final String message;
        private final boolean success;

        public AuthResponse(String message, boolean success) {
            this.message = message;
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}
