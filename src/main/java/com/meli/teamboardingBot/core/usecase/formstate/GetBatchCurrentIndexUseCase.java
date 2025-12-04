package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.ports.formstate.GetBatchCurrentIndexPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

public class GetBatchCurrentIndexUseCase implements GetBatchCurrentIndexPort {

    private final LoggerApiPort loggerApiPort;

    public GetBatchCurrentIndexUseCase(LoggerApiPort loggerApiPort) {
        this.loggerApiPort = loggerApiPort;
    }

    @Override
    public int getBatchCurrentIndex(String userId) {
        Integer index = BatchStateManager.getBatchCurrentIndex(userId);
        return index != null ? index : 0;
    }

}
