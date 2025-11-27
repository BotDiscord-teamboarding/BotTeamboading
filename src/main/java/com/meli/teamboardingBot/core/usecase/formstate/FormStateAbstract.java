package com.meli.teamboardingBot.core.usecase.formstate;

import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.domain.batch.BatchLogEntry;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FormStateAbstract {

    protected final LoggerApiPort loggerApiPort;
    protected final Map<Long, FormState> userStates = new ConcurrentHashMap<>();
    protected final Map<String, List<BatchLogEntry>> batchEntries = new ConcurrentHashMap<>();
    protected final Map<String, Integer> batchCurrentIndex = new ConcurrentHashMap<>();
    protected static final int EXPIRATION_HOURS = 2;

    public FormStateAbstract(LoggerApiPort loggerApiPort) {
        this.loggerApiPort = loggerApiPort;
    }

    protected boolean isExpired(FormState state) {
        return state.getLastActivity().isBefore(LocalDateTime.now().minusHours(EXPIRATION_HOURS));
    }
}
