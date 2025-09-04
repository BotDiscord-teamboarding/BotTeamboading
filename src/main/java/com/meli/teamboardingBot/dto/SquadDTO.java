package com.meli.teamboardingBot.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SquadDTO {
    private Long id;
    private String name;
    private String timezone;
    private String countryId;
    private Object country;
    private String languageId;
    private Object language;
    private Long trainingPlanId;
    private Object trainingPlan;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate inOrientationModeUntil;
    private List<TechnologyDTO> technologies;
    private Object project;
    private List<SquadMemberDTO> userSquads;
    private Boolean isObiSquad;
    private Object group;
    private List<String> menuItems;
    private Boolean metricConnectionsEnabled;
}
