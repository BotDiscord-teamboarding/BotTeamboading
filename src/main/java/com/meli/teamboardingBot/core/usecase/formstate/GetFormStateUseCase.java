package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.ports.formstate.GetFormStatePort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

import java.util.Locale;

public class GetFormStateUseCase extends FormStateAbstract implements GetFormStatePort {

    public GetFormStateUseCase(LoggerApiPort loggerApiPort) {
        super(loggerApiPort);
    }

    @Override
    public FormState getState(Long userId) {
        FormState state = userStates.get(userId);
        if (state != null && isExpired(state)) {
            userStates.remove(userId);
            loggerApiPort.info("Estado expirado removido para usuário: {}", userId);
            return null;
        }
        return state;
    }
}
