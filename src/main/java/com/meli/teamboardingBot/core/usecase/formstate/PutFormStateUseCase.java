package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.ports.formstate.PutFormStatePort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import org.springframework.stereotype.Component;

@Component
public class PutFormStateUseCase implements PutFormStatePort {

    private final LoggerApiPort loggerApiPort;

    public PutFormStateUseCase(LoggerApiPort loggerApiPort) {
        this.loggerApiPort = loggerApiPort;
    }

    @Override
    public void updateState(Long userId, FormState state) {
        FormStateManager.updateState(userId, state);
        loggerApiPort.info("Estado atualizado para usuário: {}", userId);
    }
}
