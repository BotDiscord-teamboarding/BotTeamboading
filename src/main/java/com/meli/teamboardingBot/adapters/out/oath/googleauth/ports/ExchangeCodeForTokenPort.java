package com.meli.teamboardingBot.adapters.out.oath.googleauth.ports;

public interface ExchangeCodeForTokenPort {
    String exchangeCodeForToken(String code, String discordUserId);
}
