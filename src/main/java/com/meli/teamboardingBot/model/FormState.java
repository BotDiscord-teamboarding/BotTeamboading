package com.meli.teamboardingBot.model;

import lombok.Getter;
import lombok.Setter;
import com.meli.teamboardingBot.enums.FormStep;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
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
    
    // Pagination state (thread-safe per user)
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
}
