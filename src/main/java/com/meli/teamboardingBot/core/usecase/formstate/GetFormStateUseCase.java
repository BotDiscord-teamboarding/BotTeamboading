package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.ports.formstate.GetFormStatePort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import com.meli.teamboardingBot.service.UserLanguageService;

import java.util.Locale;

public class GetFormStateUseCase extends FormStateAbstract implements GetFormStatePort {

    private final UserLanguageService userLanguageService;

    public GetFormStateUseCase(LoggerApiPort loggerApiPort, UserLanguageService userLanguageService) {
        super(loggerApiPort);
        this.userLanguageService = userLanguageService;
    }

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
}
