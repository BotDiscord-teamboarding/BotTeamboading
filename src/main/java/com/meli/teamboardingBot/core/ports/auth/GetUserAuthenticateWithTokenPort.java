package com.meli.teamboardingBot.core.ports.auth;

import com.meli.teamboardingBot.core.usecase.auth.UserTokenAbstract;

public interface GetUserAuthenticateWithTokenPort {
    UserTokenAbstract.AuthResponse authenticateUserWithToken(String discordUserId, String accessToken);
}
