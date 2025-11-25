package com.meli.teamboardingBot.adapters.config.beans;

import com.meli.teamboardingBot.core.ports.auth.GetUserTokenPort;
import com.meli.teamboardingBot.core.ports.auth.api.GetApiTokenPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import com.meli.teamboardingBot.core.ports.rest.RestPort;
import com.meli.teamboardingBot.core.usecase.defaultclient.GetDefaultClientUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImplBeansConfiguration {

    @Bean
    GetDefaultClientUseCase getDefaultClientUseCase (LoggerApiPort logger, GetApiTokenPort authService, GetUserTokenPort getUserTokenPort, String apiUrl, RestPort
            restPort) {
        return new GetDefaultClientUseCase(logger, authService, getUserTokenPort, apiUrl, restPort);
    }

}
