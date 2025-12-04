package com.meli.teamboardingBot.core.ports.formstate;

import com.meli.teamboardingBot.core.domain.batch.BatchLogEntry;

import java.util.List;

public interface SetBatchCurrentIndexPort {
    void setBatchCurrentIndex(String userId, int index);

}
