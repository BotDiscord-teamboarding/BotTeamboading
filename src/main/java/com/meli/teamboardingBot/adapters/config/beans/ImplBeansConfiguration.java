package com.meli.teamboardingBot.adapters.config.beans;

import com.meli.teamboardingBot.adapters.out.client.DefaultAuthenticationService;
import com.meli.teamboardingBot.core.ports.auth.GetIsUserAuthenticatedPort;
import com.meli.teamboardingBot.core.ports.auth.GetUserTokenPort;
import com.meli.teamboardingBot.core.ports.auth.api.GetApiTokenPort;
import com.meli.teamboardingBot.core.ports.auth.api.GetManualApiTokenPort;
import com.meli.teamboardingBot.core.ports.defaultclient.GetDefaultClientPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import com.meli.teamboardingBot.core.ports.rest.RestPort;
import com.meli.teamboardingBot.core.usecase.auth.*;
import com.meli.teamboardingBot.core.usecase.defaultclient.GetDefaultClientUseCase;
import com.meli.teamboardingBot.core.usecase.defaultclient.GetDefaultClientWithParamUseCase;
import com.meli.teamboardingBot.core.usecase.defaultclient.PostDefaultClientUseCase;
import com.meli.teamboardingBot.core.usecase.defaultclient.PutDefaultClientUseCase;
import com.meli.teamboardingBot.core.usecase.discorduserauthentication.LogoutDiscordUserUseCase;
import com.meli.teamboardingBot.core.usecase.discorduserauthentication.DiscordUserAuthenticationUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImplBeansConfiguration {

    @Bean
    public GetIsUserAuthenticatedUseCase getIsUserAuthenticatedUseCase(LoggerApiPort logger) {
        return new GetIsUserAuthenticatedUseCase(logger);
    }

    @Bean
    public GetUserTokenUseCase getUserTokenUseCase(LoggerApiPort logger, GetIsUserAuthenticatedPort isUserAuthenticated) {
        return new GetUserTokenUseCase(logger, isUserAuthenticated);
    }

    @Bean
    public GetDefaultClientUseCase getDefaultClientUseCase(LoggerApiPort logger, DefaultAuthenticationService authService, GetUserTokenPort getUserTokenPort, @Value("${api.url}") String apiUrl, RestPort
            restPort) {
        return new GetDefaultClientUseCase(logger, authService, getUserTokenPort, apiUrl, restPort);
    }

    @Bean
    public GetDefaultClientWithParamUseCase getDefaultClientWithParamUseCase(GetDefaultClientPort getDefaultClientPort) {
        return new GetDefaultClientWithParamUseCase(getDefaultClientPort);
    }

    @Bean
    public PostDefaultClientUseCase postDefaultClientUseCase(LoggerApiPort logger, GetApiTokenPort authService, GetUserTokenPort getUserTokenPort, @Value("${api.url}") String apiUrl, RestPort restPort) {
        return new PostDefaultClientUseCase(logger, authService, getUserTokenPort, apiUrl, restPort);
    }

    @Bean
    public PutDefaultClientUseCase putDefaultClientUseCase(LoggerApiPort logger, GetApiTokenPort authService, GetUserTokenPort getUserTokenPort, @Value("${api.url}") String apiUrl, RestPort restPort) {
        return new PutDefaultClientUseCase(logger, authService, getUserTokenPort, apiUrl, restPort);
    }

    @Bean
    GetUserAuthenticateWithTokenUseCase getUserAuthenticateWithTokenUseCase(LoggerApiPort loggerApiPort) {
        return new GetUserAuthenticateWithTokenUseCase(loggerApiPort);
    }

    @Bean
    GetUserAuthenticateUseCase getUserAuthenticateUseCase(LoggerApiPort loggerApiPort, GetManualApiTokenPort getManualApiTokenPort) {
        return new GetUserAuthenticateUseCase(loggerApiPort, getManualApiTokenPort);
    }

    @Bean
    DiscordUserAuthenticationUseCase putDiscordUserAuthenticationUseCase(LoggerApiPort loggerApiPort) {
        return new DiscordUserAuthenticationUseCase(loggerApiPort);
    }

    @Bean
    LogoutDiscordUserUseCase deleteDiscordUserAuthenticationUseCase(LoggerApiPort loggerApiPort) {
        return new LogoutDiscordUserUseCase(loggerApiPort);
    }

}
