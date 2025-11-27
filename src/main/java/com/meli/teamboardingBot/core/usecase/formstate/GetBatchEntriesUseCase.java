package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.batch.BatchLogEntry;
import com.meli.teamboardingBot.core.ports.formstate.GetBatchCurrentIndexPort;
import com.meli.teamboardingBot.core.ports.formstate.GetBatchEntriesPort;
import com.meli.teamboardingBot.core.ports.formstate.SetBatchCurrentIndexPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

import java.util.List;

public class GetBatchEntriesUseCase extends FormStateAbstract implements GetBatchEntriesPort {

    public GetBatchEntriesUseCase(LoggerApiPort loggerApiPort) {
        super(loggerApiPort);
    }

    @Override
    public List<BatchLogEntry> getBatchEntries(String userId) {
        return batchEntries.get(userId);
    }

}
