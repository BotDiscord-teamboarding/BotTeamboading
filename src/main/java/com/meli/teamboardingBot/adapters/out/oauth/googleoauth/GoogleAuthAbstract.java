package com.meli.teamboardingBot.adapters.out.oauth.googleoauth;

import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import com.meli.teamboardingBot.core.ports.rest.RestPort;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class GoogleAuthAbstract {

    protected final LoggerApiPort loggerApiPort;
    protected final RestPort restPort;
    protected final String googleConnectionUrl;
    protected final String googleLoginUrl;
    
    protected final ConcurrentHashMap<String, String> processedCodes = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<String, Long> codeTimestamps = new ConcurrentHashMap<>();

    public GoogleAuthAbstract(LoggerApiPort loggerApiPort, RestPort restPort, 
                             String googleConnectionUrl, String googleLoginUrl) {
        this.loggerApiPort = loggerApiPort;
        this.restPort = restPort;
        this.googleConnectionUrl = googleConnectionUrl;
        this.googleLoginUrl = googleLoginUrl;
    }

    protected void cleanupOldCodes() {
        long tenMinutesAgo = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
        codeTimestamps.entrySet().removeIf(entry -> {
            if (entry.getValue() < tenMinutesAgo) {
                processedCodes.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
}
