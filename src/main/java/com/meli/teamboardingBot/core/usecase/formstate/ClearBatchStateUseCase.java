package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.batch.BatchLogEntry;
import com.meli.teamboardingBot.core.ports.formstate.ClearBatchStatePort;
import com.meli.teamboardingBot.core.ports.formstate.SetBatchCurrentIndexPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

import java.util.List;

public class ClearBatchStateUseCase extends FormStateAbstract implements ClearBatchStatePort {

    public ClearBatchStateUseCase(LoggerApiPort loggerApiPort) {
        super(loggerApiPort);
    }

    @Override
    public void clearBatchState(String userId) {
        batchEntries.remove(userId);
        batchCurrentIndex.remove(userId);
        loggerApiPort.info("Batch state limpo para usuário: {}", userId);
    }
}
