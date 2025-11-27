package com.meli.teamboardingBot.core.ports.discorduserauthentication;

import com.meli.teamboardingBot.core.usecase.discorduserauthentication.DiscrdUserAuthenticationAbstract;

public interface DiscordUserAuthenticationPort {
    DiscrdUserAuthenticationAbstract.AuthResponse authenticateUserWithToken(String discordUserId, String accessToken);
}
