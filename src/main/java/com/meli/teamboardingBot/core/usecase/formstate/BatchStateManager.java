package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.batch.BatchLogEntry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BatchStateManager {
    
    private static final Map<String, List<BatchLogEntry>> batchEntries = new ConcurrentHashMap<>();
    private static final Map<String, Integer> batchCurrentIndex = new ConcurrentHashMap<>();
    private static final Map<String, LocalDateTime> batchLastActivity = new ConcurrentHashMap<>();
    private static final int EXPIRATION_HOURS = 2;
    
    private BatchStateManager() {
        // Singleton - construtor privado
    }
    
    public static List<BatchLogEntry> getBatchEntries(String userId) {
        if (isExpired(userId)) {
            clearBatchState(userId);
            return null;
        }
        updateLastActivity(userId);
        return batchEntries.get(userId);
    }
    
    public static void setBatchEntries(String userId, List<BatchLogEntry> entries) {
        if (entries != null) {
            batchEntries.put(userId, entries);
            updateLastActivity(userId);
        }
    }
    
    public static Integer getBatchCurrentIndex(String userId) {
        if (isExpired(userId)) {
            clearBatchState(userId);
            return null;
        }
        updateLastActivity(userId);
        return batchCurrentIndex.getOrDefault(userId, 0);
    }
    
    public static void setBatchCurrentIndex(String userId, Integer index) {
        if (index != null) {
            batchCurrentIndex.put(userId, index);
            updateLastActivity(userId);
        }
    }
    
    public static void clearBatchState(String userId) {
        batchEntries.remove(userId);
        batchCurrentIndex.remove(userId);
        batchLastActivity.remove(userId);
    }
    
    public static void cleanExpiredBatchStates() {
        batchLastActivity.entrySet().removeIf(entry -> {
            String userId = entry.getKey();
            if (isExpired(userId)) {
                clearBatchState(userId);
                return true;
            }
            return false;
        });
    }
    
    private static void updateLastActivity(String userId) {
        batchLastActivity.put(userId, LocalDateTime.now());
    }
    
    private static boolean isExpired(String userId) {
        LocalDateTime lastActivity = batchLastActivity.get(userId);
        return lastActivity != null && lastActivity.isBefore(LocalDateTime.now().minusHours(EXPIRATION_HOURS));
    }
}
