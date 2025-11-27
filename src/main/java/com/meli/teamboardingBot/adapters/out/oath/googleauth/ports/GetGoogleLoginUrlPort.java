package com.meli.teamboardingBot.adapters.out.oath.googleauth.ports;

public interface GetGoogleLoginUrlPort {
    String getGoogleLoginConnectionUrl(String discordUserId);
}
