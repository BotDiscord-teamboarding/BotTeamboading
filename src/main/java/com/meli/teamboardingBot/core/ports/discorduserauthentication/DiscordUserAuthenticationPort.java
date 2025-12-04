package com.meli.teamboardingBot.core.ports.discorduserauthentication;

import com.meli.teamboardingBot.core.usecase.auth.manual.ManualAuthenticationAbstract;

public interface DiscordUserAuthenticationPort {
    ManualAuthenticationAbstract.AuthResponse authenticateUserWithToken(String discordUserId, String accessToken);
}
