package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.batch.BatchLogEntry;
import com.meli.teamboardingBot.core.ports.formstate.GetBatchEntriesPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

import java.util.List;

public class GetBatchEntriesUseCase implements GetBatchEntriesPort {

    private final LoggerApiPort loggerApiPort;

    public GetBatchEntriesUseCase(LoggerApiPort loggerApiPort) {
        this.loggerApiPort = loggerApiPort;
    }

    @Override
    public List<BatchLogEntry> getBatchEntries(String userId) {
        return BatchStateManager.getBatchEntries(userId);
    }

}
