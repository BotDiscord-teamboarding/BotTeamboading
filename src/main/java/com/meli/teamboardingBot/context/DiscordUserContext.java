package com.meli.teamboardingBot.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordUserContext {
    private static final Map<Long, String> userContextMap = new ConcurrentHashMap<>();
    private static final ThreadLocal<Long> currentThreadId = new ThreadLocal<>();

    public static void setCurrentUserId(String userId) {
        long threadId = Thread.currentThread().getId();
        userContextMap.put(threadId, userId);
        currentThreadId.set(threadId);
    }

    public static String getCurrentUserId() {
        Long threadId = currentThreadId.get();
        return threadId != null ? userContextMap.get(threadId) : null;
    }

    public static void clear() {
        Long threadId = currentThreadId.get();
        if (threadId != null) {
            userContextMap.remove(threadId);
            currentThreadId.remove();
        }
    }

    public static void clearForThread(long threadId) {
        userContextMap.remove(threadId);
    }
}
