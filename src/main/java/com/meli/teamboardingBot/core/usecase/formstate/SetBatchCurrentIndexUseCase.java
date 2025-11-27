package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.ports.formstate.SetBatchCurrentIndexPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

import java.util.List;

public class SetBatchCurrentIndexUseCase extends FormStateAbstract implements SetBatchCurrentIndexPort {

    public SetBatchCurrentIndexUseCase(LoggerApiPort loggerApiPort) {
        super(loggerApiPort);
    }

    @Override
    public void setBatchCurrentIndex(String userId, int index) {
        batchCurrentIndex.put(userId, index);
        loggerApiPort.info("Batch index atualizado para usuário: {} - index: {}", userId, index);
    }

}
