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
public class ClientDTO {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("areas")
    private List<AreaDTO> areas;
    
    @JsonProperty("skill_categories")
    private List<Object> skillCategories;
    
    @JsonProperty("nomenclatures")
    private List<Object> nomenclatures;
    
    @JsonProperty("allow_rookies_to_see_knowledge_tree")
    private Boolean allowRookiesToSeeKnowledgeTree;
    
    @JsonProperty("can_access_public_contents")
    private Boolean canAccessPublicContents;

}
