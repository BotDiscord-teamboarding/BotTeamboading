package com.meli.teamboardingBot.domain.batch;

import java.util.List;

public class BatchParsingResult {
    private List<BatchLogEntry> validEntries;
    private List<String> errors;
    private int totalProcessed;

    public BatchParsingResult() {}

    public BatchParsingResult(List<BatchLogEntry> validEntries, List<String> errors, int totalProcessed) {
        this.validEntries = validEntries;
        this.errors = errors;
        this.totalProcessed = totalProcessed;
    }

    public List<BatchLogEntry> getValidEntries() { return validEntries; }
    public void setValidEntries(List<BatchLogEntry> validEntries) { this.validEntries = validEntries; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public int getTotalProcessed() { return totalProcessed; }
    public void setTotalProcessed(int totalProcessed) { this.totalProcessed = totalProcessed; }

    public boolean hasValidEntries() {
        return validEntries != null && !validEntries.isEmpty();
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public int getValidCount() {
        return validEntries != null ? validEntries.size() : 0;
    }

    public int getErrorCount() {
        return errors != null ? errors.size() : 0;
    }
}
