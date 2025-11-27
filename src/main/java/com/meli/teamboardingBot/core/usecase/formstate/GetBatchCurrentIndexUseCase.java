package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.batch.BatchLogEntry;
import com.meli.teamboardingBot.core.ports.formstate.GetBatchCurrentIndexPort;
import com.meli.teamboardingBot.core.ports.formstate.SetBatchCurrentIndexPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

import java.util.List;

public class GetBatchCurrentIndexUseCase extends FormStateAbstract implements GetBatchCurrentIndexPort {

    public GetBatchCurrentIndexUseCase(LoggerApiPort loggerApiPort) {
        super(loggerApiPort);
    }

    @Override
    public int getBatchCurrentIndex(String userId) {
        return batchCurrentIndex.getOrDefault(userId, 0);
    }

}
