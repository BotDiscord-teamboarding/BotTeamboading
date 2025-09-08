package com.meli.teamboardingBot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class SquadDTO {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("timezone")
    private String timezone;
    
    @JsonProperty("country_id")
    private String countryId;
    
    @JsonProperty("country")
    private Object country;
    
    @JsonProperty("language_id")
    private String languageId;
    
    @JsonProperty("language")
    private Object language;
    
    @JsonProperty("training_plan_id")
    private Long trainingPlanId;
    
    @JsonProperty("training_plan")
    private Object trainingPlan;
    
    @JsonProperty("start_date")
    private LocalDate startDate;
    
    @JsonProperty("end_date")
    private LocalDate endDate;
    
    @JsonProperty("in_orientation_mode_until")
    private LocalDate inOrientationModeUntil;
    
    @JsonProperty("technologies")
    private List<TechnologyDTO> technologies;
    
    @JsonProperty("project")
    private Object project;
    
    @JsonProperty("user_squads")
    private List<SquadMemberDTO> userSquads;
    
    @JsonProperty("is_obi_squad")
    private Boolean isObiSquad;
    
    @JsonProperty("group")
    private Object group;
    
    @JsonProperty("menu_items")
    private List<String> menuItems;
    
    @JsonProperty("metric_connections_enabled")
    private Boolean metricConnectionsEnabled;


}
