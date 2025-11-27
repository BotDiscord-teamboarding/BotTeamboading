package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.batch.BatchLogEntry;
import com.meli.teamboardingBot.core.ports.formstate.SetBatchCurrentIndexPort;
import com.meli.teamboardingBot.core.ports.formstate.SetBatchEntriesPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

import java.util.List;

public class SetBatchEntriesUseCase extends FormStateAbstract implements SetBatchEntriesPort {

    public SetBatchEntriesUseCase(LoggerApiPort loggerApiPort) {
        super(loggerApiPort);
    }

    @Override
    public void setBatchEntries(String userId, List<BatchLogEntry> entries) {
        batchEntries.put(userId, entries);
        loggerApiPort.info("Batch entries definidas para usuário: {} - {} entradas", userId, entries.size());
    }

}
