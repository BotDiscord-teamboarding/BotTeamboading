package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.ports.formstate.DeleteFormStatePort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import org.springframework.stereotype.Component;

@Component
public class DeleteFormStateUseCase implements DeleteFormStatePort {

    private final LoggerApiPort loggerApiPort;

    public DeleteFormStateUseCase(LoggerApiPort loggerApiPort) {
        this.loggerApiPort = loggerApiPort;
    }

    @Override
    public void removeState(Long userId) {
        FormStateManager.deleteState(userId);
        loggerApiPort.info("Estado removido para usuário: {}", userId);
    }

}
