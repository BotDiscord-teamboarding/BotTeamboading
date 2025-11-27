package com.meli.teamboardingBot.service;

import com.meli.teamboardingBot.adapters.out.client.ClientAuthBoarding;
import com.meli.teamboardingBot.adapters.dto.AuthTokenResponseDTO;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
    private final ClientAuthBoarding authClient;

    private final Map<String, String> userCredentials = new HashMap<>();

    public AuthService(ClientAuthBoarding authClient) {
        this.authClient = authClient;
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
