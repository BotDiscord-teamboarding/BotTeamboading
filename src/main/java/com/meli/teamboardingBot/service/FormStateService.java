package com.meli.teamboardingBot.service;
import com.meli.teamboardingBot.domain.FormState;
import com.meli.teamboardingBot.domain.batch.BatchLogEntry;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Service
public class FormStateService {
    private final Map<Long, FormState> userStates = new ConcurrentHashMap<>();
    private final UserLanguageService userLanguageService;
    private static final int EXPIRATION_HOURS = 2;
    
    public FormStateService(UserLanguageService userLanguageService) {
        this.userLanguageService = userLanguageService;
    }
    
    public FormState getOrCreateState(Long userId) {
        return userStates.computeIfAbsent(userId, k -> {
            FormState newState = new FormState();
            Locale userLocale = userLanguageService.getUserLanguagePreference(String.valueOf(userId));
            if (userLocale != null) {
                newState.setLocale(userLocale);
            }
            return newState;
        });
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

    private final Map<String, List<BatchLogEntry>> batchEntries = new ConcurrentHashMap<>();
    private final Map<String, Integer> batchCurrentIndex = new ConcurrentHashMap<>();

    public void setBatchEntries(String userId, List<BatchLogEntry> entries) {
        batchEntries.put(userId, entries);
    }

    public List<BatchLogEntry> getBatchEntries(String userId) {
        return batchEntries.get(userId);
    }

    public void setBatchCurrentIndex(String userId, int index) {
        batchCurrentIndex.put(userId, index);
    }

    public int getBatchCurrentIndex(String userId) {
        return batchCurrentIndex.getOrDefault(userId, 0);
    }

    public void clearBatchState(String userId) {
        batchEntries.remove(userId);
        batchCurrentIndex.remove(userId);
    }
}
