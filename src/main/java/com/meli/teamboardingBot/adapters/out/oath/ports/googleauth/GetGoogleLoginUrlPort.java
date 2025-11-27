package com.meli.teamboardingBot.adapters.out.oath.ports.googleauth;

public interface GetGoogleLoginUrlPort {
    String getGoogleLoginConnectionUrl(String discordUserId);
}
