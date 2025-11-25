package com.meli.teamboardingBot.core.ports.auth;

public interface GetIsUserAuthenticatedPort {
    boolean isUserAuthenticated(String discordUserId);
}
