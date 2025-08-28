package com.meli.teamboardingBot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@ToString
public class SquadUserDTO {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("first_name")
    private String firstName;
    
    @JsonProperty("last_name")
    private String lastName;
    
    @JsonProperty("user_squads")
    private List<UserSquadDTO> userSquads;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("picture")
    private String picture;
    
    @JsonProperty("credit_usages")
    private List<Object> creditUsages;
    
    @JsonProperty("preferred_language_id")
    private String preferredLanguageId;
    
    @JsonProperty("preferred_language")
    private LanguageDTO preferredLanguage;
    
    @JsonProperty("technologies")
    private List<TechnologyDTO> technologies;
    
    @JsonProperty("user_skill_trees")
    private Object userSkillTrees;
}
