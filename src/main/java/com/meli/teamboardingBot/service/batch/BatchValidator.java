package com.meli.teamboardingBot.service.batch;

import com.meli.teamboardingBot.model.batch.BatchLogEntry;
import com.meli.teamboardingBot.model.batch.BatchParsingResult;
import java.util.List;

public interface BatchValidator {
    BatchParsingResult validateEntries(List<BatchLogEntry> entries);
    boolean isValidSquad(String squadName);
    boolean isValidUser(String userName, Long squadId);
    boolean isValidType(String typeName);
    boolean isValidCategory(String categoryName);
}
