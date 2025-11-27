package com.meli.teamboardingBot.core.ports.formstate;

import com.meli.teamboardingBot.core.domain.batch.BatchLogEntry;

import java.util.List;

public interface BatchStatePort {
    void setBatchEntries(String userId, List<BatchLogEntry> entries);
    List<BatchLogEntry> getBatchEntries(String userId);
    void setBatchCurrentIndex(String userId, int index);
    int getBatchCurrentIndex(String userId);
    void clearBatchState(String userId);
}
