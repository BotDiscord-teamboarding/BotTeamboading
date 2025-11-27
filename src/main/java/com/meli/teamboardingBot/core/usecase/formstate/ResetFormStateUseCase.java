package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.ports.formstate.ResetFormStatePort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

public class ResetFormStateUseCase extends FormStateAbstract implements ResetFormStatePort {

    public ResetFormStateUseCase(LoggerApiPort loggerApiPort) {
        super(loggerApiPort);
    }

    @Override
    public void resetState(Long userId) {
        FormState state = userStates.get(userId);
        if (state != null) {
            state.reset();
            loggerApiPort.info("Estado resetado para usuário: {}", userId);
        }
    }

}
