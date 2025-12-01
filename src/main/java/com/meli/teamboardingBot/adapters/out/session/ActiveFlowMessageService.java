package com.meli.teamboardingBot.adapters.out.session;

import net.dv8tion.jda.api.interactions.InteractionHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ActiveFlowMessageService {

    private static final Logger logger = LoggerFactory.getLogger(ActiveFlowMessageService.class);
    private final Map<Long, InteractionHook> activeFlowHooks = new ConcurrentHashMap<>();

    public void registerFlowHook(Long userId, InteractionHook hook) {
        activeFlowHooks.put(userId, hook);
        logger.info("📌 Hook de fluxo registrado para usuário: {}", userId);
    }

    public InteractionHook getFlowHook(Long userId) {
        return activeFlowHooks.get(userId);
    }

    public void clearFlowHook(Long userId) {
        activeFlowHooks.remove(userId);
        logger.info("🧹 Hook de fluxo removido para usuário: {}", userId);
    }

    public boolean hasActiveFlow(Long userId) {
        return activeFlowHooks.containsKey(userId);
    }
}
