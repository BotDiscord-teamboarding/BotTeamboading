package com.meli.teamboardingBot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço para rastrear o canal de interação original do usuário
 * Usado para enviar respostas no mesmo canal onde o comando foi executado
 */
@Service
public class UserInteractionChannelService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserInteractionChannelService.class);
    
    // Mapa: discordUserId -> channelId
    private final Map<String, String> userChannels = new ConcurrentHashMap<>();
    
    // Mapa: discordUserId -> messageId (para editar a mensagem original)
    private final Map<String, String> userMessages = new ConcurrentHashMap<>();
    
    /**
     * Registra o canal onde o usuário iniciou a interação
     */
    public void registerUserChannel(String discordUserId, String channelId, String messageId) {
        userChannels.put(discordUserId, channelId);
        userMessages.put(discordUserId, messageId);
        logger.info("📍 Canal registrado para usuário {}: canal={}, mensagem={}", 
            discordUserId, channelId, messageId);
    }
    
    /**
     * Obtém o ID do canal onde o usuário iniciou a interação
     */
    public String getUserChannelId(String discordUserId) {
        return userChannels.get(discordUserId);
    }
    
    /**
     * Obtém o ID da mensagem original do usuário
     */
    public String getUserMessageId(String discordUserId) {
        return userMessages.get(discordUserId);
    }
    
    /**
     * Remove o registro do canal do usuário
     */
    public void clearUserChannel(String discordUserId) {
        userChannels.remove(discordUserId);
        userMessages.remove(discordUserId);
        logger.info("🧹 Canal removido para usuário {}", discordUserId);
    }
    
    /**
     * Verifica se existe um canal registrado para o usuário
     */
    public boolean hasUserChannel(String discordUserId) {
        return userChannels.containsKey(discordUserId);
    }
}
