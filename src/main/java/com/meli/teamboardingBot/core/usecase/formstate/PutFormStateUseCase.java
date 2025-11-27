package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.ports.formstate.PutFormStatePort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

public class PutFormStateUseCase extends FormStateAbstract implements PutFormStatePort {

    public PutFormStateUseCase(LoggerApiPort loggerApiPort) {
        super(loggerApiPort);
    }

    @Override
    public void updateState(Long userId, FormState state) {
        userStates.put(userId, state);
        loggerApiPort.info("Estado atualizado para usuário: {}", userId);
    }
}
