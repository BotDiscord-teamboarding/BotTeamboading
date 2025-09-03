package com.meli.teamboardingBot.service;
import com.meli.teamboardingBot.model.FormState;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Service
public class FormStateService {
    private final Map<Long, FormState> userStates = new ConcurrentHashMap<>();
    private static final int EXPIRATION_HOURS = 2;
    public FormState getOrCreateState(Long userId) {
        return userStates.computeIfAbsent(userId, k -> new FormState());
    }
    public FormState getState(Long userId) {
        FormState state = userStates.get(userId);
        if (state != null && isExpired(state)) {
            userStates.remove(userId);
            return null;
        }
        return state;
    }
    public void updateState(Long userId, FormState state) {
        userStates.put(userId, state);
    }
    public void removeState(Long userId) {
        userStates.remove(userId);
    }
    public void resetState(Long userId) {
        FormState state = userStates.get(userId);
        if (state != null) {
            state.reset();
        }
    }
    private boolean isExpired(FormState state) {
        return state.getLastActivity().isBefore(LocalDateTime.now().minusHours(EXPIRATION_HOURS));
    }
    public void cleanExpiredStates() {
        userStates.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }
}
