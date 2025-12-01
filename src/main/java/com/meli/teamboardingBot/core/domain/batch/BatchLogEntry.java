package com.meli.teamboardingBot.core.domain.batch;

import java.time.LocalDate;
import java.util.List;

public class BatchLogEntry {
    private String squadName;
    private String personName;
    private String logType;
    private List<String> categories;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private int lineNumber;
    private Long squadId;
    private Long userId;
    private Long typeId;
    private List<Long> categoryIds;
    private String modifiedField;

    public BatchLogEntry() {}

    public BatchLogEntry(String squadName, String personName, String logType, 
                        List<String> categories, String description, 
                        LocalDate startDate, LocalDate endDate, int lineNumber) {
        this.squadName = squadName;
        this.personName = personName;
        this.logType = logType;
        this.categories = categories;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.lineNumber = lineNumber;
    }

    public String getSquadName() { return squadName; }
    public void setSquadName(String squadName) { this.squadName = squadName; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }

    public String getLogType() { return logType; }
    public void setLogType(String logType) { this.logType = logType; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

    public Long getSquadId() { return squadId; }
    public void setSquadId(Long squadId) { this.squadId = squadId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }

    public List<Long> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<Long> categoryIds) { this.categoryIds = categoryIds; }

    public String getModifiedField() { return modifiedField; }
    public void setModifiedField(String modifiedField) { this.modifiedField = modifiedField; }
}
