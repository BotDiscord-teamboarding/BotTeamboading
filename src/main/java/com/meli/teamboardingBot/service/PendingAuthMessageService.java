package com.meli.teamboardingBot.service;

import net.dv8tion.jda.api.entities.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PendingAuthMessageService {
    
    private static final Logger logger = LoggerFactory.getLogger(PendingAuthMessageService.class);
    private final Map<String, Message> pendingAuthMessages = new ConcurrentHashMap<>();

    public void storePendingAuthMessage(String userId, Message message) {
        pendingAuthMessages.put(userId, message);
        logger.info("Mensagem de autenticação pendente armazenada para usuário: {}", userId);
    }

    public void clearPendingAuthMessage(String userId) {
        Message message = pendingAuthMessages.remove(userId);
        if (message != null) {
            try {
                message.delete().queue(
                    success -> logger.info("Mensagem de autenticação pendente apagada para usuário: {}", userId),
                    error -> logger.warn("Erro ao apagar mensagem de autenticação para usuário {}: {}", userId, error.getMessage())
                );
            } catch (Exception e) {
                logger.error("Erro ao tentar apagar mensagem para usuário {}: {}", userId, e.getMessage());
            }
        }
    }

    public boolean hasPendingAuthMessage(String userId) {
        return pendingAuthMessages.containsKey(userId);
    }
}
