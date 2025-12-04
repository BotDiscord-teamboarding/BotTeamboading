package com.meli.teamboardingBot.core.domain;


import com.meli.teamboardingBot.core.domain.enums.FormStep;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class FormState {
    private FormStep step = FormStep.INITIAL;
    private boolean isCreating = false;
    private boolean isEditing = false;
    private LocalDateTime lastActivity = LocalDateTime.now();
    private String squadId;
    private String squadName;
    private String userId;
    private String userName;
    private String typeId;
    private String typeName;
    private List<String> categoryIds = new ArrayList<>();
    private List<String> categoryNames = new ArrayList<>();
    private String description;
    private String startDate;
    private String endDate;
    private Long squadLogId;
    private String language = "es=ES";
    private Locale locale;

    private int currentPage = 1;
    private int totalPages = 1;

    public void setStep(FormStep step) {
        this.step = step;
        this.lastActivity = LocalDateTime.now();
    }
    public void reset() {
        this.step = FormStep.INITIAL;
        this.isCreating = false;
        this.isEditing = false;
        this.squadId = null;
        this.squadName = null;
        this.userId = null;
        this.userName = null;
        this.typeId = null;
        this.typeName = null;
        this.categoryIds.clear();
        this.categoryNames.clear();
        this.description = null;
        this.startDate = null;
        this.endDate = null;
        this.squadLogId = null;
        this.currentPage = 1;
        this.totalPages = 1;
        this.lastActivity = LocalDateTime.now();
    }

    public FormStep getStep() {
        return step;
    }

    public boolean isCreating() {
        return isCreating;
    }

    public void setCreating(boolean creating) {
        isCreating = creating;
    }

    public boolean isEditing() {
        return isEditing;
    }

    public void setEditing(boolean editing) {
        isEditing = editing;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    public String getSquadId() {
        return squadId;
    }

    public void setSquadId(String squadId) {
        this.squadId = squadId;
    }

    public String getSquadName() {
        return squadName;
    }

    public void setSquadName(String squadName) {
        this.squadName = squadName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public List<String> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<String> categoryIds) {
        this.categoryIds = categoryIds;
    }

    public List<String> getCategoryNames() {
        return categoryNames;
    }

    public void setCategoryNames(List<String> categoryNames) {
        this.categoryNames = categoryNames;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Long getSquadLogId() {
        return squadLogId;
    }

    public void setSquadLogId(Long squadLogId) {
        this.squadLogId = squadLogId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

}
