package com.meli.teamboardingBot.core.ports.formstate;

import com.meli.teamboardingBot.core.domain.FormState;

public interface GetFormStatePort {
    FormState getOrCreateState(Long userId);
    FormState getState(Long userId);
}
