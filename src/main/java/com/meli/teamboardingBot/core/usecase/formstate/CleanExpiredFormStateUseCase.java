package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.ports.formstate.CleanFormStatePort;
import com.meli.teamboardingBot.core.ports.formstate.DeleteFormStatePort;
import com.meli.teamboardingBot.core.ports.formstate.ResetFormStatePort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

public class CleanExpiredFormStateUseCase extends FormStateAbstract implements CleanFormStatePort {

    public CleanExpiredFormStateUseCase(LoggerApiPort loggerApiPort) {
        super(loggerApiPort);
    }


    @Override
    public void cleanExpiredStates() {
        int removedCount = 0;
        for (var entry : userStates.entrySet()) {
            if (isExpired(entry.getValue())) {
                userStates.remove(entry.getKey());
                removedCount++;
            }
        }
        if (removedCount > 0) {
            loggerApiPort.info("Estados expirados limpos: {} estados removidos", removedCount);
        }
    }
}
