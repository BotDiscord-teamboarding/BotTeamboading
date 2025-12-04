package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.ports.formstate.GetFormStatePort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import org.springframework.stereotype.Component;

@Component
public class GetFormStateUseCase implements GetFormStatePort {

    private final LoggerApiPort loggerApiPort;

    public GetFormStateUseCase(LoggerApiPort loggerApiPort) {
        this.loggerApiPort = loggerApiPort;
    }

    @Override
    public FormState getState(Long userId) {
        FormState state = FormStateManager.getState(userId);
        if (state == null) {
            loggerApiPort.info("Estado não encontrado ou expirado para usuário: {}", userId);
        }
        return state;
    }
}
