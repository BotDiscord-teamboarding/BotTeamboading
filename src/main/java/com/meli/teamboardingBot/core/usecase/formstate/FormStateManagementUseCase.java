package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.domain.batch.BatchLogEntry;
import com.meli.teamboardingBot.core.ports.formstate.BatchStatePort;
import com.meli.teamboardingBot.core.ports.formstate.DeleteFormStatePort;
import com.meli.teamboardingBot.core.ports.formstate.GetFormStatePort;
import com.meli.teamboardingBot.core.ports.formstate.PutFormStatePort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import com.meli.teamboardingBot.service.UserLanguageService;

import java.util.List;
import java.util.Locale;

public class FormStateManagementUseCase extends FormStateAbstract 
        implements GetFormStatePort, PutFormStatePort, DeleteFormStatePort, BatchStatePort {

    private final UserLanguageService userLanguageService;

    public FormStateManagementUseCase(LoggerApiPort loggerApiPort, UserLanguageService userLanguageService) {
        super(loggerApiPort);
        this.userLanguageService = userLanguageService;
    }

    // GetFormStatePort methods
    @Override
    public FormState getOrCreateState(Long userId) {
        return userStates.computeIfAbsent(userId, k -> {
            FormState newState = new FormState();
            Locale userLocale = userLanguageService.getUserLanguagePreference(String.valueOf(userId));
            if (userLocale != null) {
                newState.setLocale(userLocale);
            }
            return newState;
        });
    }

    @Override
    public FormState getState(Long userId) {
        FormState state = userStates.get(userId);
        if (state != null && isExpired(state)) {
            userStates.remove(userId);
            loggerApiPort.info("Estado expirado removido para usuário: {}", userId);
            return null;
        }
        return state;
    }

    // PutFormStatePort methods
    @Override
    public void updateState(Long userId, FormState state) {
        userStates.put(userId, state);
        loggerApiPort.info("Estado atualizado para usuário: {}", userId);
    }

    // DeleteFormStatePort methods
    @Override
    public void removeState(Long userId) {
        userStates.remove(userId);
        loggerApiPort.info("Estado removido para usuário: {}", userId);
    }

    @Override
    public void resetState(Long userId) {
        FormState state = userStates.get(userId);
        if (state != null) {
            state.reset();
            loggerApiPort.info("Estado resetado para usuário: {}", userId);
        }
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

    // BatchStatePort methods
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
