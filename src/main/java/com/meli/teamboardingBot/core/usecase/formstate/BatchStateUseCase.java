package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.batch.BatchLogEntry;
import com.meli.teamboardingBot.core.ports.formstate.BatchStatePort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

import java.util.List;

public class BatchStateUseCase extends FormStateAbstract implements BatchStatePort {

    public BatchStateUseCase(LoggerApiPort loggerApiPort) {
        super(loggerApiPort);
    }

    @Override
    public void setBatchEntries(String userId, List<BatchLogEntry> entries) {
        batchEntries.put(userId, entries);
        loggerApiPort.info("Batch entries definidas para usuário: {} - {} entradas", userId, entries.size());
    }

    @Override
    public List<BatchLogEntry> getBatchEntries(String userId) {
        return batchEntries.get(userId);
    }

    @Override
    public void setBatchCurrentIndex(String userId, int index) {
        batchCurrentIndex.put(userId, index);
        loggerApiPort.info("Batch index atualizado para usuário: {} - index: {}", userId, index);
    }

    @Override
    public int getBatchCurrentIndex(String userId) {
        return batchCurrentIndex.getOrDefault(userId, 0);
    }

    @Override
    public void clearBatchState(String userId) {
        batchEntries.remove(userId);
        batchCurrentIndex.remove(userId);
        loggerApiPort.info("Batch state limpo para usuário: {}", userId);
    }
}
