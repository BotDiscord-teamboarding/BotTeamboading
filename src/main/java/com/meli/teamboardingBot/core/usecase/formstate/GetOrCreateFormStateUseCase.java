package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.ports.formstate.GetOrCreateFormStatePort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import com.meli.teamboardingBot.adapters.out.language.UserLanguageService;

import java.util.Locale;

public class GetOrCreateFormStateUseCase extends FormStateAbstract implements GetOrCreateFormStatePort {

    private final UserLanguageService userLanguageService;

    public GetOrCreateFormStateUseCase(LoggerApiPort loggerApiPort, UserLanguageService userLanguageService) {
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
}
