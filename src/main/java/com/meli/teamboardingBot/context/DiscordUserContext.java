package com.meli.teamboardingBot.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordUserContext {
    // Mapa thread-safe para armazenar o ID do usuário por thread
    private static final Map<Long, String> userContextMap = new ConcurrentHashMap<>();
    // ThreadLocal para manter o ID da thread atual
    private static final ThreadLocal<Long> currentThreadId = new ThreadLocal<>();

    /**
     * Define o ID do usuário para a thread atual
     * @param userId ID do usuário do Discord
     */
    public static void setCurrentUserId(String userId) {
        long threadId = Thread.currentThread().getId();
        userContextMap.put(threadId, userId);
        currentThreadId.set(threadId);
    }

    /**
     * Obtém o ID do usuário atual baseado na thread
     * @return ID do usuário ou null se não estiver definido
     */
    public static String getCurrentUserId() {
        Long threadId = currentThreadId.get();
        return threadId != null ? userContextMap.get(threadId) : null;
    }

    /**
     * Limpa o contexto do usuário para a thread atual
     */
    public static void clear() {
        Long threadId = currentThreadId.get();
        if (threadId != null) {
            userContextMap.remove(threadId);
            currentThreadId.remove();
        }
    }

    /**
     * Limpa o contexto para uma thread específica
     * Útil para limpar o contexto de threads que podem ter terminado sem chamar clear()
     * @param threadId ID da thread a ser limpa
     */
    public static void clearForThread(long threadId) {
        userContextMap.remove(threadId);
    }
}
