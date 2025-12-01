package com.meli.teamboardingBot.adapters.out.oauth.googleoauth.ports;

public interface ExchangeCodeForTokenPort {
    String exchangeCodeForToken(String code, String discordUserId);
}
