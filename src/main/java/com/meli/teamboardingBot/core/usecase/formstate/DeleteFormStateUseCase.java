package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.FormState;
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

    @Override
    public void resetState(Long userId) {
        FormState state = userStates.get(userId);
        if (state != null) {
            state.reset();
            loggerApiPort.info("Estado resetado para usuário: {}", userId);
        }
    }

    @Override
    public void cleanExpiredStates() {
        int removedCount = 0;
        for (var entry : userStates.entrySet()) {
            if (isExpired(entry.getValue())) {
                userStates.remove(entry.getKey());
                removedCount++;
            }
        }
        if (removedCount > 0) {
            loggerApiPort.info("Estados expirados limpos: {} estados removidos", removedCount);
        }
    }
}
