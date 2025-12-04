package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.ports.formstate.ResetFormStatePort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import org.springframework.stereotype.Component;

@Component
public class ResetFormStateUseCase implements ResetFormStatePort {

    private final LoggerApiPort loggerApiPort;

    public ResetFormStateUseCase(LoggerApiPort loggerApiPort) {
        this.loggerApiPort = loggerApiPort;
    }

    @Override
    public void resetState(Long userId) {
        FormState state = FormStateManager.getState(userId);
        if (state != null) {
            state.reset();
            FormStateManager.updateState(userId, state);
            loggerApiPort.info("Estado resetado para usuário: {}", userId);
        }
    }

}
