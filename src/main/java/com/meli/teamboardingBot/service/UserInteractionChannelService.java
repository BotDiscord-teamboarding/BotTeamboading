package com.meli.teamboardingBot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserInteractionChannelService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserInteractionChannelService.class);
    
    private final Map<String, String> userChannels = new ConcurrentHashMap<>();
    
    private final Map<String, String> userMessages = new ConcurrentHashMap<>();

    public void registerUserChannel(String discordUserId, String channelId, String messageId) {
        userChannels.put(discordUserId, channelId);
        userMessages.put(discordUserId, messageId);
        logger.info("📍 Canal registrado para usuário {}: canal={}, mensagem={}", 
            discordUserId, channelId, messageId);
    }
    
    public String getUserChannelId(String discordUserId) {
        return userChannels.get(discordUserId);
    }
    
    public String getUserMessageId(String discordUserId) {
        return userMessages.get(discordUserId);
    }
    
    public void clearUserChannel(String discordUserId) {
        userChannels.remove(discordUserId);
        userMessages.remove(discordUserId);
        logger.info("🧹 Canal removido para usuário {}", discordUserId);
    }
    
    public boolean hasUserChannel(String discordUserId) {
        return userChannels.containsKey(discordUserId);
    }
}
