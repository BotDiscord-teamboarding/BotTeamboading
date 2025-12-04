package com.meli.teamboardingBot.core.ports.auth;

import com.meli.teamboardingBot.core.usecase.auth.oath.UserTokenAbstract;

public interface GetUserAuthenticatePort {
    UserTokenAbstract.AuthResponse authenticateUser(String discordUserId, String username, String password);
}
