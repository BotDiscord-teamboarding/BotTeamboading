package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.ports.formstate.DeleteFormStatePort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

public class DeleteFormStateUseCase extends FormStateAbstract implements DeleteFormStatePort {

    public DeleteFormStateUseCase(LoggerApiPort loggerApiPort) {
        super(loggerApiPort);
    }

    @Override
    public void removeState(Long userId) {
        userStates.remove(userId);
        loggerApiPort.info("Estado removido para usuário: {}", userId);
    }

}
