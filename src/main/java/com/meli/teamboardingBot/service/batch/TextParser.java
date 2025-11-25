package com.meli.teamboardingBot.service.batch;

import com.meli.teamboardingBot.domain.batch.BatchLogEntry;
import java.util.List;

public interface TextParser {
    List<BatchLogEntry> parseText(String inputText);
    boolean canParse(String inputText);
    String getParserName();
}
