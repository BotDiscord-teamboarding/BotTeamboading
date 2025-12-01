package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.batch.BatchLogEntry;
import com.meli.teamboardingBot.core.ports.formstate.SetBatchEntriesPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

import java.util.List;

public class SetBatchEntriesUseCase implements SetBatchEntriesPort {

    private final LoggerApiPort loggerApiPort;

    public SetBatchEntriesUseCase(LoggerApiPort loggerApiPort) {
        this.loggerApiPort = loggerApiPort;
    }

    @Override
    public void setBatchEntries(String userId, List<BatchLogEntry> entries) {
        BatchStateManager.setBatchEntries(userId, entries);
        loggerApiPort.info("Batch entries definidas para usuário: {} - {} entradas", userId, entries != null ? entries.size() : 0);
    }

}
