package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.ports.formstate.GetOrCreateFormStatePort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import com.meli.teamboardingBot.adapters.out.language.UserLanguageService;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class GetOrCreateFormStateUseCase implements GetOrCreateFormStatePort {

    private final LoggerApiPort loggerApiPort;
    private final UserLanguageService userLanguageService;

    public GetOrCreateFormStateUseCase(LoggerApiPort loggerApiPort, UserLanguageService userLanguageService) {
        this.loggerApiPort = loggerApiPort;
        this.userLanguageService = userLanguageService;
    }

    @Override
    public FormState getOrCreateState(Long userId) {
        FormState state = FormStateManager.getState(userId);
        if (state == null) {
            state = new FormState();
            Locale userLocale = userLanguageService.getUserLanguagePreference(String.valueOf(userId));
            if (userLocale != null) {
                state.setLocale(userLocale);
            }
            FormStateManager.updateState(userId, state);
        }
        return state;
    }
}
