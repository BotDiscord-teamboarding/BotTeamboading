package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.FormState;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FormStateManager {
    
    private static final Map<Long, FormState> userStates = new ConcurrentHashMap<>();
    private static final int EXPIRATION_HOURS = 2;
    
    private FormStateManager() {
    }
    
    public static FormState getState(Long userId) {
        FormState state = userStates.get(userId);
        if (state != null && isExpired(state)) {
            userStates.remove(userId);
            return null;
        }
        return state;
    }
    
    public static void updateState(Long userId, FormState state) {
        if (state != null) {
            state.setLastActivity(LocalDateTime.now());
            userStates.put(userId, state);
        }
    }
    
    public static void deleteState(Long userId) {
        userStates.remove(userId);
    }
    
    public static void resetState(Long userId) {
        userStates.remove(userId);
    }
    
    public static void cleanExpiredStates() {
        userStates.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }
    
    private static boolean isExpired(FormState state) {
        return state.getLastActivity().isBefore(LocalDateTime.now().minusHours(EXPIRATION_HOURS));
    }
}
