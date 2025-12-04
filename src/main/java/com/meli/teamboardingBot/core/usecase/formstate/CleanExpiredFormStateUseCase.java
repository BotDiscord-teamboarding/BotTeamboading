package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.ports.formstate.CleanFormStatePort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import org.springframework.stereotype.Component;

@Component
public class CleanExpiredFormStateUseCase implements CleanFormStatePort {

    private final LoggerApiPort loggerApiPort;

    public CleanExpiredFormStateUseCase(LoggerApiPort loggerApiPort) {
        this.loggerApiPort = loggerApiPort;
    }

    @Override
    public void cleanExpiredStates() {
        FormStateManager.cleanExpiredStates();
        loggerApiPort.info("Limpeza de estados expirados executada");
    }
}
