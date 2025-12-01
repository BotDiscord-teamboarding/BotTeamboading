package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.ports.formstate.SetBatchCurrentIndexPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

public class SetBatchCurrentIndexUseCase implements SetBatchCurrentIndexPort {

    private final LoggerApiPort loggerApiPort;

    public SetBatchCurrentIndexUseCase(LoggerApiPort loggerApiPort) {
        this.loggerApiPort = loggerApiPort;
    }

    @Override
    public void setBatchCurrentIndex(String userId, int index) {
        BatchStateManager.setBatchCurrentIndex(userId, index);
        loggerApiPort.info("Batch index atualizado para usuário: {} - index: {}", userId, index);
    }

}
