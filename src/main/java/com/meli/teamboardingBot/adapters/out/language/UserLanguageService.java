package com.meli.teamboardingBot.adapters.out.language;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserLanguageService {
    private static final Logger logger = LoggerFactory.getLogger(UserLanguageService.class);
    
    private final Map<String, Locale> userLanguagePreferences = new ConcurrentHashMap<>();
    
    private static final Locale PORTUGUESE = Locale.forLanguageTag("pt-BR");
    private static final Locale SPANISH = Locale.forLanguageTag("es-ES");
    private static final Locale DEFAULT_LOCALE = SPANISH;
    
    public Locale detectUserLocale(String discordLocale) {
        if (discordLocale == null || discordLocale.isEmpty()) {
            logger.info("Discord locale is null or empty, using default: {}", DEFAULT_LOCALE);
            return DEFAULT_LOCALE;
        }
        
        logger.info("Detecting locale from Discord locale: {}", discordLocale);
        
        String lowerLocale = discordLocale.toLowerCase();
        
        if (lowerLocale.startsWith("pt")) {
            logger.info("Portuguese detected from locale: {}", discordLocale);
            return PORTUGUESE;
        } else if (lowerLocale.startsWith("es")) {
            logger.info("Spanish detected from locale: {}", discordLocale);
            return SPANISH;
        } else {
            logger.info("Unsupported locale: {}, using default: {}", discordLocale, DEFAULT_LOCALE);
            return DEFAULT_LOCALE;
        }
    }
    
    public void saveUserLanguagePreference(String userId, Locale locale) {
        userLanguagePreferences.put(userId, locale);
        logger.info("Language preference saved for user {}: {}", userId, locale);
    }
    
    public Locale getUserLanguagePreference(String userId) {
        return userLanguagePreferences.get(userId);
    }
    
    public boolean hasLanguagePreference(String userId) {
        return userLanguagePreferences.containsKey(userId);
    }
    
    public void clearUserLanguagePreference(String userId) {
        userLanguagePreferences.remove(userId);
        logger.info("Language preference cleared for user: {}", userId);
    }
    
    public Locale getAlternativeLocale(Locale currentLocale) {
        if (currentLocale.equals(PORTUGUESE)) {
            return SPANISH;
        } else {
            return PORTUGUESE;
        }
    }
    
    public String getLanguageName(Locale locale) {
        if (locale.equals(PORTUGUESE)) {
            return "Português";
        } else if (locale.equals(SPANISH)) {
            return "Español";
        }
        return "Español";
    }
    
    public Locale getPortugueseLocale() {
        return PORTUGUESE;
    }
    
    public Locale getSpanishLocale() {
        return SPANISH;
    }
}
