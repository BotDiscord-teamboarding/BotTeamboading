package com.meli.teamboardingBot.adapters.out.oath.ports.googleauth;

public interface ExchangeCodeForTokenPort {
    String exchangeCodeForToken(String code, String discordUserId);
}
