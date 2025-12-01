package com.meli.teamboardingBot.adapters.out.oauth.googleoauth.ports;

public interface GetGoogleLoginUrlPort {
    String getGoogleLoginConnectionUrl(String discordUserId);
}
