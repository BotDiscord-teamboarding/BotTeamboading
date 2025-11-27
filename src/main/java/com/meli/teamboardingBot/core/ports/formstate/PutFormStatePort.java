package com.meli.teamboardingBot.core.ports.formstate;

import com.meli.teamboardingBot.core.domain.FormState;

public interface PutFormStatePort {
    void updateState(Long userId, FormState state);
}
