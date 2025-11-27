package com.meli.teamboardingBot.core.ports.formstate;

public interface DeleteFormStatePort {
    void removeState(Long userId);
    void resetState(Long userId);
    void cleanExpiredStates();
}
