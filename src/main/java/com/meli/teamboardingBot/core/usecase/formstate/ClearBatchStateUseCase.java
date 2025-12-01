package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.ports.formstate.ClearBatchStatePort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

public class ClearBatchStateUseCase implements ClearBatchStatePort {

    private final LoggerApiPort loggerApiPort;

    public ClearBatchStateUseCase(LoggerApiPort loggerApiPort) {
        this.loggerApiPort = loggerApiPort;
    }

    @Override
    public void clearBatchState(String userId) {
        BatchStateManager.clearBatchState(userId);
        loggerApiPort.info("Batch state limpo para usuário: {}", userId);
    }
}
